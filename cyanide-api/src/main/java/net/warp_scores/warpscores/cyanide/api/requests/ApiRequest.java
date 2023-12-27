package net.warp_scores.warpscores.cyanide.api.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.DigestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiRequest<RequestType, ResponseType> {

    private final String requestPath;
    private final Class<RequestType> requestClass;
    private final Class<ResponseType> responseClass;

    public enum Platform {pc, playstation, xbox}

    public enum Order {ID, LastMatchDate, CreationDate}

    private Platform platform = Platform.pc;
    private Integer limit;
    private Integer exact;

    private Duration cacheValidity = CacheValidityDurations.TWO_HOURS;
    private Duration readTimeout = null;
    private Duration connectTimeout = Duration.ofSeconds(1);

    public String md5Sum() {
        MultiValueMap<String, String> queryParams = toQueryParams();
        String queryParamsAsString = queryParams.toSingleValueMap()
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> String.join("=", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(","));
        StringBuilder md5Sum = new StringBuilder();
        DigestUtils.appendMd5DigestAsHex(queryParamsAsString.getBytes(StandardCharsets.UTF_8), md5Sum);
        return md5Sum.toString();
    }

    public MultiValueMap<String, String> toQueryParams() {
        Stream<Method> methods = relevantGetterMethodsAsStream();
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        methods.forEach(method -> addQueryParamsFor(queryParams, method));
        return queryParams;
    }

    private void addQueryParamsFor(MultiValueMap<String, String> queryParams, Method method) {
        char[] charArray = method.getName().substring(3).toCharArray();
        charArray[0] = Character.toLowerCase(charArray[0]);
        String key = new String(charArray);
        try {
            Object value = method.invoke(this);
            if (value != null) {
                String stringValue = String.valueOf(value);
                if (!stringValue.trim().isEmpty()) {
                    queryParams.add(key, stringValue);
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            // ignored
        }
    }

    private Stream<Method> relevantGetterMethodsAsStream() {
        Method[] allDeclaredMethods = ReflectionUtils.getAllDeclaredMethods(requestClass);
        return Arrays.stream(allDeclaredMethods)
                .filter(method -> method.getName().startsWith("get"))
                .filter(method -> !Arrays.asList("getClass", "getCacheValidity", "getReadTimeout", "getConnectTimeout",
                                "getResponseClass", "getRequestPath", "getRequestClass")
                        .contains(method.getName()));
    }
}
