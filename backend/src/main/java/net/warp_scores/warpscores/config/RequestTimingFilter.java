package net.warp_scores.warpscores.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestTimingFilter extends OncePerRequestFilter {
    public static final String REQUEST_START_NANOS =
            RequestTimingFilter.class.getName() + ".startNanos";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        request.setAttribute(REQUEST_START_NANOS, System.nanoTime());
        filterChain.doFilter(request, response);
    }
}
