package cz.semester.courseapp.http;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiRequestLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
            logCompletedRequest(request, response, startedAt);
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "event=api_request_failed method={} path={} error=\"{}\"",
                    request.getMethod(),
                    request.getRequestURI(),
                    exception.getMessage(),
                    exception);
            throw exception;
        }
    }

    private void logCompletedRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            long startedAt) {
        long durationMs = System.currentTimeMillis() - startedAt;
        if (response.getStatus() >= 400) {
            LOGGER.warn(
                    "event=api_request method={} path={} status={} durationMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    durationMs);
            return;
        }
        LOGGER.info(
                "event=api_request method={} path={} status={} durationMs={}",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                durationMs);
    }
}
