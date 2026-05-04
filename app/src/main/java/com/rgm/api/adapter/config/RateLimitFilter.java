package com.rgm.api.adapter.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
  private static final int MAX_CACHE_SIZE = 10_000;

  private final int maxRequests;
  private final int windowSeconds;
  private final Map<String, RateLimitEntry> cache = new ConcurrentHashMap<>();
  private final AtomicInteger requestCounter = new AtomicInteger(0);

  public RateLimitFilter(
      @Value("${rate-limit.max-requests:10}") final int maxRequests,
      @Value("${rate-limit.window-seconds:60}") final int windowSeconds) {
    this.maxRequests = maxRequests;
    this.windowSeconds = windowSeconds;
  }

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

    evictExpiredEntries();

    final String clientIp = request.getRemoteAddr();
    final Instant now = Instant.now();
    final RateLimitEntry entry =
        cache.compute(
            clientIp,
            (key, existing) -> {
              if (existing == null || existing.isExpired(now, windowSeconds)) {
                return new RateLimitEntry(now, 1);
              }
              existing.increment();
              return existing;
            });

    if (entry.getCount() > maxRequests) {
      log.warn("Rate limit exceeded for IP: {}", clientIp);
      response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      response.setContentType("application/json");
      response
          .getWriter()
          .write(
              "{\"status\":429,\"error\":\"Too Many Requests\","
                  + "\"message\":\"Limite de requisicoes excedido. Tente novamente em "
                  + windowSeconds
                  + " segundos.\"}");
      return;
    }

    filterChain.doFilter(request, response);
  }

  private void evictExpiredEntries() {
    if (requestCounter.incrementAndGet() % 100 == 0 || cache.size() > MAX_CACHE_SIZE) {
      final Instant now = Instant.now();
      cache.entrySet().removeIf(e -> e.getValue().isExpired(now, windowSeconds));
    }
  }

  private static final class RateLimitEntry {
    private final Instant windowStart;
    private volatile int count;

    RateLimitEntry(final Instant windowStart, final int count) {
      this.windowStart = windowStart;
      this.count = count;
    }

    boolean isExpired(final Instant now, final int windowSeconds) {
      return now.isAfter(windowStart.plusSeconds(windowSeconds));
    }

    void increment() {
      count++;
    }

    int getCount() {
      return count;
    }
  }
}
