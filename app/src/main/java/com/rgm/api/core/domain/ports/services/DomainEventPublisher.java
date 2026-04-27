package com.rgm.api.core.domain.ports.services;

public interface DomainEventPublisher {

  void publish(Object event);
}
