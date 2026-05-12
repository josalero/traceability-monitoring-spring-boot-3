package com.example.commerce.commons.observation;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@AutoConfigureAfter(ObservationAutoConfiguration.class)
@ConditionalOnClass(ObservationRegistryCustomizer.class)
@ConditionalOnBean({ObservationRegistry.class, MeterRegistry.class})
public class ObservationErrorMetricsAutoConfiguration {

  @Bean
  ObservationRegistryCustomizer<ObservationRegistry> commerceObservationErrorMetrics(
      MeterRegistry meterRegistry) {
    return registry ->
        registry.observationConfig().observationHandler(new ObservationErrorCountingHandler(meterRegistry));
  }
}
