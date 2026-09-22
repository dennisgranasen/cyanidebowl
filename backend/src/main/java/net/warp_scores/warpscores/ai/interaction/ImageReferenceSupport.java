package net.warp_scores.warpscores.ai.interaction;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

final class ImageReferenceSupport {
    record Loaded(byte[] bytes, String contentType, String filename) {}

    private static final long MAX_SOURCE_BYTES = 8L * 1024 * 1024;
    private static final long MAX_PIXELS = 20_000_000L;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    List<Loaded> load(List<String> references, int max) throws Exception {
        if (references == null || references.isEmpty()) return List.of();
        if (references.size() > max)
            throw new IllegalArgumentException("At most " + max + " reference images are supported");
        var result = new ArrayList<Loaded>();
        for (int i = 0; i < references.size(); i++) {
            result.add(normalize(read(references.get(i)), "reference-" + i + ".png"));
        }
        return List.copyOf(result);
    }

    private byte[] read(String reference) throws Exception {
        URI uri = URI.create(reference);
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            Path path = Path.of(uri).toAbsolutePath().normalize();
            long size = Files.size(path);
            if (size <= 0 || size > MAX_SOURCE_BYTES)
                throw new IllegalArgumentException("Reference image is too large");
            return Files.readAllBytes(path);
        }
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))
            throw new IllegalArgumentException("Unsupported reference image URI");

        for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
            if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                    || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                    || address.isMulticastAddress())
                throw new IllegalArgumentException("Private reference image hosts are not allowed");
        }

        var request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).GET().build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new IllegalArgumentException("Reference image download failed with HTTP " + response.statusCode());
        if (response.body().length == 0 || response.body().length > MAX_SOURCE_BYTES)
            throw new IllegalArgumentException("Reference image is too large");
        return response.body();
    }

    private Loaded normalize(byte[] source, String filename) throws Exception {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(source));
        if (original == null) throw new IllegalArgumentException("Reference is not a supported image");
        if ((long) original.getWidth() * original.getHeight() > MAX_PIXELS)
            throw new IllegalArgumentException("Reference image has too many pixels");

        int width = original.getWidth();
        int height = original.getHeight();
        double scale = Math.min(1.0, 511.0 / Math.max(width, height));
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage normalized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = normalized.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }

        var output = new ByteArrayOutputStream();
        ImageIO.write(normalized, "png", output);
        return new Loaded(output.toByteArray(), "image/png", filename);
    }
}
