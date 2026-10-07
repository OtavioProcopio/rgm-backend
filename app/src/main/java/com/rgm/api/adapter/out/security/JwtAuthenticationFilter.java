package com.rgm.api.adapter.out.security;

import com.rgm.api.core.application.usecases.auth.AutenticarAcessoUseCase;
import com.rgm.api.core.domain.exceptions.NaoAutorizadoException;
import com.rgm.api.core.domain.model.aggregates.Usuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  private static final String PREFIXO = "Bearer ";

  private final AutenticarAcessoUseCase autenticarAcessoUseCase;

  public JwtAuthenticationFilter(final AutenticarAcessoUseCase autenticarAcessoUseCase) {
    this.autenticarAcessoUseCase = autenticarAcessoUseCase;
  }

  @Override
  protected void doFilterInternal(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final FilterChain filterChain)
      throws ServletException, IOException {

    final String header = request.getHeader("Authorization");
    if (header != null && header.startsWith(PREFIXO)) {
      try {
        final Usuario usuario = autenticarAcessoUseCase.execute(header.substring(PREFIXO.length()));

        // A autoridade vem do perfil atual do usuario, nao do perfil gravado no token.
        final UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(
                usuario.getId().toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name())));

        SecurityContextHolder.getContext().setAuthentication(auth);
      } catch (final NaoAutorizadoException e) {
        log.debug("Token rejeitado: {}", e.getMessage());
      }
    }
    filterChain.doFilter(request, response);
  }
}
