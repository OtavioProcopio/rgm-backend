package com.rgm.api.adapter.out.security;

import com.rgm.api.core.domain.model.aggregates.Usuario;
import com.rgm.api.core.domain.ports.repositories.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  private final SecretKey key;
  private final UsuarioRepository usuarioRepository;

  public JwtAuthenticationFilter(
      @Value("${jwt.secret}") final String secret, final UsuarioRepository usuarioRepository) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.usuarioRepository = usuarioRepository;
  }

  @Override
  protected void doFilterInternal(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final FilterChain filterChain)
      throws ServletException, IOException {

    final String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      try {
        final String token = header.substring(7);
        final Claims claims =
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        final String type = claims.get("type", String.class);
        if (!"access".equals(type)) {
          log.debug("Token rejeitado: type='{}' (esperado 'access')", type);
          filterChain.doFilter(request, response);
          return;
        }

        final String userId = claims.getSubject();
        final String perfil = claims.get("perfil", String.class);

        final boolean usuarioAtivo =
            usuarioRepository.findById(UUID.fromString(userId)).map(Usuario::isAtivo).orElse(false);
        if (!usuarioAtivo) {
          log.debug("Token rejeitado: usuario {} inativo ou inexistente", userId);
          filterChain.doFilter(request, response);
          return;
        }

        final UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + perfil)));

        SecurityContextHolder.getContext().setAuthentication(auth);
      } catch (final io.jsonwebtoken.ExpiredJwtException e) {
        log.debug("Token JWT expirado: {}", e.getMessage());
      } catch (final Exception e) {
        log.warn("Falha ao validar token JWT: {}", e.getMessage());
      }
    }
    filterChain.doFilter(request, response);
  }
}
