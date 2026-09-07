package net.warp_scores.warpscores.service;

import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.*;
import net.warp_scores.warpscores.model.UserPermissions;
import net.warp_scores.warpscores.model.WarpScoresUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static net.warp_scores.warpscores.model.Permissions.*;

@Service("userPermissionService")
@RequiredArgsConstructor
public class UserPermissionService {
    private final WarpScoresUserRepository users;
    private final SeasonRepository seasons;
    private final PhaseRepository phases;
    private final StageRepository stages;
    private final StageSourceRepository stageSources;
    private final RegisteredSourceRepository registeredSources;

    public UserPermissions permissions(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwt)) {
            return UserPermissions.noPermissions();
        }

        WarpScoresUser user = users.findByAuthSubject(jwt.getToken().getSubject()).orElse(null);
        boolean siteAdmin = hasAuthority(authentication, WRITE_SITE_ADMIN)
                || user != null && Boolean.TRUE.equals(user.getSiteAdmin());
        boolean globalLeagueAdmin = siteAdmin
                || hasAuthority(authentication, WRITE_LEAGUE_ADMIN)
                || user != null && Boolean.TRUE.equals(user.getLeagueAdmin());
        boolean registerLeague = siteAdmin
                || hasAuthority(authentication, WRITE_REGISTER_LEAGUE)
                || user != null && Boolean.TRUE.equals(user.getRegisterLeague());

        Set<String> scoped = new LinkedHashSet<>();
        if (user != null && user.getAdminForLeagueSystems() != null) {
            scoped.addAll(user.getAdminForLeagueSystems());
        }

        return new UserPermissions(
                true,
                globalLeagueAdmin || !scoped.isEmpty(),
                siteAdmin,
                registerLeague,
                globalLeagueAdmin,
                List.copyOf(scoped));
    }

    public boolean isSiteAdmin(Authentication authentication) {
        return permissions(authentication).isWriteSiteAdmin();
    }

    public boolean isGlobalLeagueAdmin(Authentication authentication) {
        return permissions(authentication).isGlobalLeagueAdmin();
    }

    public boolean hasAnyLeagueAdmin(Authentication authentication) {
        return permissions(authentication).isWriteLeagueAdmin();
    }

    public boolean canRegisterLeague(Authentication authentication) {
        return permissions(authentication).isWriteRegisterLeague();
    }

    public boolean canAdminLeagueSystem(Authentication authentication, String leagueSystemId) {
        UserPermissions permissions = permissions(authentication);
        return permissions.isGlobalLeagueAdmin()
                || leagueSystemId != null && permissions.getAdminForLeagueSystems().contains(leagueSystemId);
    }

    public boolean canAdminSeason(Authentication authentication, String seasonId) {
        return seasons.findById(seasonId)
                .map(season -> canAdminLeagueSystem(authentication, season.getLeagueSystemId()))
                .orElse(false);
    }

    public boolean canAdminPhase(Authentication authentication, String phaseId) {
        return phases.findById(phaseId)
                .map(phase -> canAdminLeagueSystem(authentication, phase.getLeagueSystemId()))
                .orElse(false);
    }

    public boolean canAdminStage(Authentication authentication, String stageId) {
        return stages.findById(stageId)
                .map(stage -> canAdminLeagueSystem(authentication, stage.getLeagueSystemId()))
                .orElse(false);
    }

    public boolean canAdminStageSource(Authentication authentication, String sourceId) {
        return stageSources.findById(sourceId)
                .map(source -> canAdminLeagueSystem(authentication, source.getLeagueSystemId()))
                .orElse(false);
    }

    public boolean canAdminRegisteredSource(Authentication authentication, String sourceId) {
        return registeredSources.findById(sourceId)
                .map(source -> canAdminLeagueSystem(authentication, source.getLeagueSystemId()))
                .orElse(false);
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }
}
