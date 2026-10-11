package com.rgm.api.adapter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cadeia de segurança dedicada ao Actuator. A conta de coleta é distinta de qualquer usuário humano
 * e vem de variável de ambiente ({@code monitoramento.coleta.usuario} e {@code
 * monitoramento.coleta.senha}). Sem a senha configurada, ninguém coleta métricas: o repositório de
 * usuários fica vazio e toda requisição protegida recebe 401. Apenas {@code /actuator/health} é
 * público.
 */
@Configuration
public class ActuatorSecurityConfig {

  private static final String PAPEL_COLETOR = "COLETOR";

  @Bean
  @Order(1)
  public SecurityFilterChain actuatorSecurityFilterChain(
      final HttpSecurity http,
      @Value("${monitoramento.coleta.usuario:}") final String usuario,
      @Value("${monitoramento.coleta.senha:}") final String senha)
      throws Exception {
    return http.securityMatcher("/actuator/**")
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .anyRequest()
                    .hasRole(PAPEL_COLETOR))
        .httpBasic(Customizer.withDefaults())
        .userDetailsService(criarUserDetailsService(usuario, senha))
        .build();
  }

  private static UserDetailsService criarUserDetailsService(
      final String usuario, final String senha) {
    if (usuario == null || usuario.isBlank() || senha == null || senha.isBlank()) {
      return new InMemoryUserDetailsManager();
    }
    final String senhaCodificada =
        PasswordEncoderFactories.createDelegatingPasswordEncoder().encode(senha);
    return new InMemoryUserDetailsManager(
        User.withUsername(usuario).password(senhaCodificada).roles(PAPEL_COLETOR).build());
  }
}
