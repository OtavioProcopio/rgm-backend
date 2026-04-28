package com.rgm.api.adapter.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private static final int MAX_REQUESTS = 10;
  private static final long WINDOW_SECONDS = 60;
  private static final int MAX_ENTRIES = 10_000;
  private static final int CLEANUP_INTERVAL = 100;

  private final ConcurrentMap<String, RateWindow> clients = new ConcurrentHashMap<>();
  private final AtomicInteger requestCounter = new AtomicInteger(0);

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

    evictIfNeeded();

    final String clientIp = request.getRemoteAddr();
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

  private void evictIfNeeded() {
    final int count = requestCounter.incrementAndGet();
    if (count % CLEANUP_INTERVAL == 0 || clients.size() > MAX_ENTRIES) {
      final Instant now = Instant.now();
      clients.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }
  }

  private record RateWindow(Instant windowStart, int count) {
    boolean isExpired(final Instant now) {
      return now.isAfter(windowStart.plusSeconds(WINDOW_SECONDS));
    }
  }
}
