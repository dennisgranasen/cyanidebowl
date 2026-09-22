package net.warp_scores.warpscores.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class LeagueSystemAdminWebConfig implements WebMvcConfigurer {
    private final LeagueSystemAdminInterceptor leagueSystemAdminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(leagueSystemAdminInterceptor).addPathPatterns(
                "/admin/league-systems/**",
                "/admin/seasons/**",
                "/admin/phases/**",
                "/admin/stages/**",
                "/admin/stage-sources/**",
                "/admin/match-selections/**",
                "/admin/registered-sources/**",
                "/admin/cyanide-competitions/**");
    }
}
