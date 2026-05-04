package com.rgm.api.adapter.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {

  private final RateLimitFilter filter = new RateLimitFilter(10, 60, "*");

  @Test
  void devePermitirRequestsAbaixoDoLimite() throws Exception {
    final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
    final MockHttpServletResponse response = new MockHttpServletResponse();
    final MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertEquals(200, response.getStatus());
  }

  @Test
  void deveBloqueApos10Requests() throws Exception {
    for (int i = 0; i < 10; i++) {
      final MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
      req.setRemoteAddr("192.168.1.100");
      filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
    }

    final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
    request.setRemoteAddr("192.168.1.100");
    final MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertEquals(429, response.getStatus());
  }

  @Test
  void naoDeveAplicarRateLimitEmOutrosEndpoints() throws Exception {
    for (int i = 0; i < 20; i++) {
      final MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/solicitacoes");
      req.setRemoteAddr("192.168.1.200");
      final MockHttpServletResponse resp = new MockHttpServletResponse();
      filter.doFilter(req, resp, new MockFilterChain());
      assertEquals(200, resp.getStatus());
    }
  }
}
