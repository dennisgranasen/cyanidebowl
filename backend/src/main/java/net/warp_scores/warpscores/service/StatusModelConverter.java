package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.cyanide.api.responses.StatusResponse;
import net.warp_scores.warpscores.domain.model.Status;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.stream.Collectors;

@Service
public class StatusModelConverter {

    public Status toStatus(StatusResponse.Game game) {
        Status status = new Status();
        status.setGameName(game.getName());
        status.setOverall(game.getStatus().isOk());
        status.setServiceStatuses(game.getService_statuses());
        status.setMaintenance(toMaintenance(game.getMaintenance()));
        status.setSocialLinks(game.getSocial_links());
        status.setPlatforms(toPlatforms(game.getStatus().getPlatforms()));
        status.setNews(toNews(game.getNews()));
        return status;
    }

    private Status.Platform[] toPlatforms(StatusResponse.Platform[] responsePlatforms) {
        return Arrays.stream(responsePlatforms)
                .map(this::toPlatform)
                .collect(Collectors.toList())
                .toArray(new Status.Platform[0]);
    }

    private Status.Platform toPlatform(StatusResponse.Platform responsePlatform) {
        Status.Platform platform = new Status.Platform();
        PopulatorUtil.copyNonNullProperties(responsePlatform, platform);
        return platform;
    }

    private Status.Maintenance toMaintenance(StatusResponse.Maintenance responseMaintenance) {
        Status.Maintenance maintenance = new Status.Maintenance();
        PopulatorUtil.copyNonNullProperties(responseMaintenance, maintenance);
        return maintenance;
    }

    private Status.News[] toNews(StatusResponse.News[] responseNews) {
        return Arrays.stream(responseNews).map(this::toNews).toList().toArray(new Status.News[0]);
    }

    private Status.News toNews(StatusResponse.News responseNews) {
        Status.News news = new Status.News();
        PopulatorUtil.copyNonNullProperties(responseNews, news);
        return news;
    }
}
