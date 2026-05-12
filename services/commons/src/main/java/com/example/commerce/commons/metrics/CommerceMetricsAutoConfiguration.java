package com.example.commerce.commons.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@AutoConfigureAfter(ObservationAutoConfiguration.class)
@ConditionalOnClass({MeterRegistry.class, Aspect.class})
public class CommerceMetricsAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  CommerceMeterNames commerceMeterNames(
      @Value("${spring.application.name:unknown-service}") String applicationName) {
    return CommerceMeterNames.fromSpringApplicationName(applicationName);
  }

  @Bean
  @ConditionalOnBean({MeterRegistry.class, ObservationRegistry.class})
  @ConditionalOnMissingBean
  CommerceMeteredAspect commerceMeteredAspect(
      MeterRegistry meterRegistry,
      ObservationRegistry observationRegistry,
      CommerceMeterNames meterNames) {
    return new CommerceMeteredAspect(meterRegistry, observationRegistry, meterNames);
  }
}
