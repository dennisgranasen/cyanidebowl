package net.warp_scores.warpscores.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.warp_scores.warpscores.domain.persistence.*;
import net.warp_scores.warpscores.service.UserPermissionService;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class LeagueSystemAdminInterceptor implements HandlerInterceptor {
    private final UserPermissionService permissions;
    private final SeasonRepository seasons;
    private final PhaseRepository phases;
    private final StageRepository stages;
    private final StageSourceRepository stageSources;
    private final RegisteredSourceRepository registeredSources;

    @Override
    @SuppressWarnings("unchecked")
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (permissions.isGlobalLeagueAdmin(authentication)) return true;

        String path = request.getRequestURI();
        if ("/admin/league-systems".equals(path)) {
            if (HttpMethod.GET.matches(request.getMethod())) return permissions.hasAnyLeagueAdmin(authentication) || forbidden(response);
            return forbidden(response);
        }
        if (path.startsWith("/admin/cyanide-competitions/")) {
            return permissions.hasAnyLeagueAdmin(authentication) || forbidden(response);
        }

        Map<String, String> vars = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (vars == null) return forbidden(response);

        String leagueSystemId = vars.get("leagueSystemId");
        if (leagueSystemId == null && vars.get("seasonId") != null) {
            leagueSystemId = seasons.findById(vars.get("seasonId")).map(s -> s.getLeagueSystemId()).orElse(null);
        }
        if (leagueSystemId == null && vars.get("phaseId") != null) {
            leagueSystemId = phases.findById(vars.get("phaseId")).map(p -> p.getLeagueSystemId()).orElse(null);
        }
        if (leagueSystemId == null && vars.get("stageId") != null) {
            leagueSystemId = stages.findById(vars.get("stageId")).map(s -> s.getLeagueSystemId()).orElse(null);
        }
        if (leagueSystemId == null && vars.get("selectionId") != null) {
            leagueSystemId = stageSources.findById(vars.get("selectionId")).map(s -> s.getLeagueSystemId()).orElse(null);
        }
        if (leagueSystemId == null && vars.get("sourceId") != null) {
            String id = vars.get("sourceId");
            leagueSystemId = stageSources.findById(id).map(s -> s.getLeagueSystemId())
                    .or(() -> registeredSources.findById(id).map(s -> s.getLeagueSystemId()))
                    .orElse(null);
        }

        if (leagueSystemId != null && permissions.canAdminLeagueSystem(authentication, leagueSystemId)) return true;
        return forbidden(response);
    }

    private boolean forbidden(HttpServletResponse response) throws Exception {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
        return false;
    }
}
