package com.rgm.api.core.domain.ports.services;

public interface PasswordHasher {

  String hash(String rawPassword);

  boolean matches(String rawPassword, String hashedPassword);
}
