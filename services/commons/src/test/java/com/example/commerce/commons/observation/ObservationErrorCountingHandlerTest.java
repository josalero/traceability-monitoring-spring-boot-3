package com.example.commerce.commons.observation;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;

class ObservationErrorCountingHandlerTest {

  @Test
  void incrementsCounterWhenObservationStopsWithError() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    ObservationRegistry obsRegistry = ObservationRegistry.create();
    obsRegistry.observationConfig().observationHandler(new ObservationErrorCountingHandler(registry));

    var observation =
        Observation.createNotStarted("commerce.test.failure", obsRegistry).start();
    observation.error(new IllegalStateException("planned"));
    observation.stop();

    assertThat(
            registry.counter(
                    ObservationErrorCountingHandler.METRIC_NAME,
                    ObservationErrorCountingHandler.TAG_OBSERVATION,
                    "commerce.test.failure")
                .count())
        .isEqualTo(1);
  }

  @Test
  void doesNotIncrementWhenObservationSucceeds() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    ObservationRegistry obsRegistry = ObservationRegistry.create();
    obsRegistry.observationConfig().observationHandler(new ObservationErrorCountingHandler(registry));

    Observation.createNotStarted("commerce.test.ok", obsRegistry).observe(() -> {});

    assertThat(registry.find(ObservationErrorCountingHandler.METRIC_NAME).counters()).isEmpty();
  }
}
