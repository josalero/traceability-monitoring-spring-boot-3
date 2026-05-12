package com.example.commerce.commons.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

class CommerceMeteredAspectTest {

  private SimpleMeterRegistry meterRegistry;
  private ObservationRegistry observationRegistry;
  private final List<String> startedObservations = new ArrayList<>();
  private final List<Throwable> recordedErrors = new ArrayList<>();
  private final CommerceMeterNames meterNames =
      CommerceMeterNames.fromSpringApplicationName("order-service");

  Sample sample;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    observationRegistry = ObservationRegistry.create();
    startedObservations.clear();
    recordedErrors.clear();
    observationRegistry
        .observationConfig()
        .observationHandler(
            new ObservationHandler<>() {
              @Override
              public boolean supportsContext(io.micrometer.observation.Observation.Context ctx) {
                return true;
              }

              @Override
              public void onStart(io.micrometer.observation.Observation.Context context) {
                startedObservations.add(context.getName());
              }

              @Override
              public void onError(io.micrometer.observation.Observation.Context context) {
                if (context.getError() != null) {
                  recordedErrors.add(context.getError());
                }
              }
            });

    AspectJProxyFactory factory = new AspectJProxyFactory(new Sample());
    factory.addAspect(new CommerceMeteredAspect(meterRegistry, observationRegistry, meterNames));
    sample = factory.getProxy();
  }

  @Test
  void wraps_method_in_observation_and_applies_static_tags() {
    sample.tagged();

    assertThat(startedObservations).containsExactly("commerce.order_service.tagged.op");
    // Counter is not auto-incremented; the developer must call MeteredOutcomes.outcome(...).
    assertThat(meterRegistry.getMeters()).isEmpty();
  }

  @Test
  void outcome_helper_emits_counter_and_tags_observation() {
    sample.observed("hit");
    sample.observed("miss");
    sample.observed("miss");

    String name = "commerce.order_service.sample.observed";
    assertThat(meterRegistry.counter(name, "result", "hit").count()).isEqualTo(1.0);
    assertThat(meterRegistry.counter(name, "result", "miss").count()).isEqualTo(2.0);
  }

  @Test
  void method_failure_records_error_on_observation_and_propagates() {
    assertThatThrownBy(() -> sample.failing())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("boom");

    assertThat(recordedErrors).hasSize(1).first().isInstanceOf(IllegalStateException.class);
    // Outcome counter was not called inside the throwing method.
    assertThat(meterRegistry.getMeters()).isEmpty();
  }

  @Test
  void method_failure_with_logFailures_false_still_records_observation_error() {
    assertThatThrownBy(() -> sample.failingQuiet())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("boom");

    assertThat(recordedErrors).hasSize(1).first().isInstanceOf(IllegalStateException.class);
    assertThat(meterRegistry.getMeters()).isEmpty();
  }

  @Test
  void nested_metered_calls_bind_outcomes_to_correct_scope() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    ObservationRegistry reg = ObservationRegistry.create();
    CommerceMeterNames names = CommerceMeterNames.fromSpringApplicationName("order-service");
    CommerceMeteredAspect aspect = new CommerceMeteredAspect(registry, reg, names);

    Inner innerTarget = new Inner();
    AspectJProxyFactory innerFactory = new AspectJProxyFactory(innerTarget);
    innerFactory.addAspect(aspect);
    Inner inner = innerFactory.getProxy();

    Outer outerTarget = new Outer(inner);
    AspectJProxyFactory outerFactory = new AspectJProxyFactory(outerTarget);
    outerFactory.addAspect(aspect);
    Outer outer = outerFactory.getProxy();

    outer.run();

    assertThat(registry.counter("commerce.order_service.inner.op", "result", "inner").count())
        .isEqualTo(1);
    assertThat(registry.counter("commerce.order_service.outer.op", "result", "outer").count())
        .isEqualTo(1);
  }

  @Test
  void outcome_helper_outside_metered_scope_is_a_noop() {
    MeteredOutcomes.outcome("hit");
    assertThat(meterRegistry.getMeters()).isEmpty();
  }

  static class Sample {

    @CommerceMetered(value = "tagged.op", tags = {"feature", "checkout"})
    void tagged() {}

    @CommerceMetered("sample.observed")
    void observed(String outcome) {
      MeteredOutcomes.outcome(outcome);
    }

    @CommerceMetered(value = "sample.failing", logFailuresAt = Level.WARN)
    void failing() {
      throw new IllegalStateException("boom");
    }

    @CommerceMetered(value = "sample.failing.quiet", logFailures = false)
    void failingQuiet() {
      throw new IllegalStateException("boom");
    }
  }

  static class Outer {

    private final Inner inner;

    Outer(Inner inner) {
      this.inner = inner;
    }

    @CommerceMetered("outer.op")
    void run() {
      inner.work();
      MeteredOutcomes.outcome("outer");
    }
  }

  static class Inner {

    @CommerceMetered("inner.op")
    void work() {
      MeteredOutcomes.outcome("inner");
    }
  }
}
