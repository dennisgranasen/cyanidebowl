package net.warp_scores.warpscores.service;

import net.warp_scores.warpscores.cyanide.api.responses.StatusResponse;
import net.warp_scores.warpscores.domain.model.Status;
import org.springframework.stereotype.Service;

@Service
public class StatusModelConverter {

    public Status toStatus(StatusResponse.Game game) {
        Status status = new Status();
        status.setGameName(game.getName());
        status.setOverall(game.getStatus().isOk());
        status.setMaintenance(toMaintenance(game.getMaintenance()));
        status.setSocialLinks(game.getSocial_links());

        return status;
    }

    private Status.Maintenance toMaintenance(StatusResponse.Maintenance responseMaintenance) {
        Status.Maintenance maintenance = new Status.Maintenance();
        PopulatorUtil.copyNonNullProperties(responseMaintenance, maintenance);
        return maintenance;
    }
}
