package com.rgm.api.adapter.out.security;

import com.rgm.api.core.domain.ports.services.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordHasher implements PasswordHasher {

  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  @Override
  public String hash(final String rawPassword) {
    return encoder.encode(rawPassword);
  }

  @Override
  public boolean matches(final String rawPassword, final String hashedPassword) {
    return encoder.matches(rawPassword, hashedPassword);
  }
}
