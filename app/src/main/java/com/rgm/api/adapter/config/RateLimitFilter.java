package com.rgm.api.adapter.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private static final int MAX_REQUESTS = 10;
  private static final long WINDOW_SECONDS = 60;

  private final ConcurrentMap<String, RateWindow> clients = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final FilterChain filterChain)
      throws ServletException, IOException {

    if (!"/api/auth/login".equals(request.getRequestURI())) {
      filterChain.doFilter(request, response);
      return;
    }

    final String clientIp = getClientIp(request);
    final RateWindow window =
        clients.compute(
            clientIp,
            (key, existing) -> {
              final Instant now = Instant.now();
              if (existing == null || existing.isExpired(now)) {
                return new RateWindow(now, 1);
              }
              return new RateWindow(existing.windowStart, existing.count + 1);
            });

    if (window.count > MAX_REQUESTS) {
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType("application/json");
      response
          .getWriter()
          .write(
              "{\"status\":429,\"error\":\"Too Many Requests\","
                  + "\"message\":\"Limite de tentativas excedido. Tente novamente em 1 minuto.\"}");
      return;
    }

    filterChain.doFilter(request, response);
  }

  private String getClientIp(final HttpServletRequest request) {
    final String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private record RateWindow(Instant windowStart, int count) {
    boolean isExpired(final Instant now) {
      return now.isAfter(windowStart.plusSeconds(WINDOW_SECONDS));
    }
  }
}
