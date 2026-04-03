package nto.infrastructure.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FailedRequestLoggingFilter extends OncePerRequestFilter {

    private static final String LOGGED_ATTRIBUTE =
        FailedRequestLoggingFilter.class.getName() + ".LOGGED";

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.nanoTime();

        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            logIfNeeded(request, response, startedAt, ex);
            throw ex;
        } finally {
            logIfNeeded(request, response, startedAt, null);
        }
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }

    private void logIfNeeded(HttpServletRequest request,
                             HttpServletResponse response,
                             long startedAt,
                             Exception ex) {
        if (Boolean.TRUE.equals(request.getAttribute(LOGGED_ATTRIBUTE))) {
            return;
        }

        int status = ex == null ? response.getStatus() : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        if (status < HttpServletResponse.SC_BAD_REQUEST) {
            return;
        }

        request.setAttribute(LOGGED_ATTRIBUTE, Boolean.TRUE);

        long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
        String path = resolvePath(request);

        if (status >= HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
            if (ex != null) {
                log.error("Request failed: status={} method={} path={} durationMs={}",
                    status, request.getMethod(), path, durationMs, ex);
                return;
            }

            log.error("Request failed: status={} method={} path={} durationMs={}",
                status, request.getMethod(), path, durationMs);
            return;
        }

        log.warn("Request failed: status={} method={} path={} durationMs={}",
            status, request.getMethod(), path, durationMs);
    }

    private String resolvePath(HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (queryString == null || queryString.isBlank()) {
            return request.getRequestURI();
        }
        return request.getRequestURI() + "?" + queryString;
    }
}
