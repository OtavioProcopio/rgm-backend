package com.rgm.api.adapter.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita tarefas agendadas, como o heartbeat das conexoes SSE. */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
