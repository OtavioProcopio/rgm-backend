package com.rgm.api.adapter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rgm.api.adapter.out.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtFilter;
  private final RateLimitFilter rateLimitFilter;
  private final ObjectMapper objectMapper;

  public SecurityConfig(
      final JwtAuthenticationFilter jwtFilter,
      final RateLimitFilter rateLimitFilter,
      final ObjectMapper objectMapper) {
    this.jwtFilter = jwtFilter;
    this.rateLimitFilter = rateLimitFilter;
    this.objectMapper = objectMapper;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/auth/**")
                    .permitAll()
                    .requestMatchers("/api/solicitacoes/events")
                    .permitAll()
                    .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers("/api/solicitacoes/metricas", "/api/solicitacoes/metricas/**")
                    .hasAnyRole("ADMINISTRADOR", "GESTOR")
                    .requestMatchers(
                        org.springframework.http.HttpMethod.GET,
                        "/api/admin/usuarios",
                        "/api/admin/usuarios/**")
                    .hasAnyRole("ADMINISTRADOR", "GESTOR")
                    .requestMatchers("/api/admin/**")
                    .hasAnyRole("ADMINISTRADOR")
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(new JsonAuthenticationEntryPoint(objectMapper))
                    .accessDeniedHandler(new JsonAccessDeniedHandler(objectMapper)))
        .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
