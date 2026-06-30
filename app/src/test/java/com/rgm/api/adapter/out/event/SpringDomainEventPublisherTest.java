package com.rgm.api.adapter.out.event;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SpringDomainEventPublisherTest {

  @Mock private ApplicationEventPublisher applicationEventPublisher;

  @InjectMocks private SpringDomainEventPublisher publisher;

  @Test
  void devePublicarEventoNoApplicationEventPublisher() {
    final Object event = new Object();

    publisher.publish(event);

    verify(applicationEventPublisher).publishEvent(event);
  }
}
