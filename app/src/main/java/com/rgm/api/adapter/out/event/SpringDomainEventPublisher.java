package com.rgm.api.adapter.out.event;

import com.rgm.api.core.domain.ports.services.DomainEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {

  private final ApplicationEventPublisher publisher;

  public SpringDomainEventPublisher(final ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  @Override
  public void publish(final Object event) {
    publisher.publishEvent(event);
  }
}
