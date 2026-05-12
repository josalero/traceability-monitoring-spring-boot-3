package com.example.commerce.commons.observation;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;

/**
 * Increments {@code commerce.observation.errors} when an observation stops with a non-null
 * {@link Observation.Context#getError() error} (including {@code Observation#error(Throwable)}).
 */
public final class ObservationErrorCountingHandler implements ObservationHandler<Observation.Context> {

  static final String METRIC_NAME = "commerce.observation.errors";
  static final String TAG_OBSERVATION = "observation";

  private final MeterRegistry registry;

  public ObservationErrorCountingHandler(MeterRegistry registry) {
    this.registry = registry;
  }

  @Override
  public boolean supportsContext(Observation.Context context) {
    return true;
  }

  @Override
  public void onStop(Observation.Context context) {
    if (context.getError() == null) {
      return;
    }
    String name = context.getName();
    if (name == null || name.isEmpty()) {
      name = "unknown";
    }
    registry.counter(METRIC_NAME, TAG_OBSERVATION, name).increment();
  }
}
