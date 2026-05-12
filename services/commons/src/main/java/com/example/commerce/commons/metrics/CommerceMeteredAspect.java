package com.example.commerce.commons.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/**
 * Spring AOP aspect that implements {@link CommerceMetered}. Wraps the join point in an
 * Observation, applies static tags, exposes a {@link MeteredOutcomes} context for inline outcome
 * counters, and logs thrown exceptions when {@link CommerceMetered#logFailures()} is {@code true}.
 */
@Aspect
public class CommerceMeteredAspect {

  private static final Logger log = LoggerFactory.getLogger(CommerceMeteredAspect.class);

  private final MeterRegistry meterRegistry;
  private final ObservationRegistry observationRegistry;
  private final CommerceMeterNames meterNames;

  public CommerceMeteredAspect(
      MeterRegistry meterRegistry,
      ObservationRegistry observationRegistry,
      CommerceMeterNames meterNames) {
    this.meterRegistry = meterRegistry;
    this.observationRegistry = observationRegistry;
    this.meterNames = meterNames;
  }

  @Around(value = "@annotation(annotation)", argNames = "pjp,annotation")
  public Object aroundMeteredMethod(ProceedingJoinPoint pjp, CommerceMetered annotation)
      throws Throwable {
    String name = meterNames.prefix(annotation.value());
    Observation observation = Observation.createNotStarted(name, observationRegistry);
    applyStaticTags(annotation, observation);
    observation.start();
    MeteredOutcomes.push(new MeteredOutcomes.Context(name, meterRegistry, observation));
    try (var ignored = observation.openScope()) {
      return pjp.proceed();
    } catch (Throwable ex) {
      observation.error(ex);
      logFailure(annotation, pjp, name, ex);
      throw ex;
    } finally {
      MeteredOutcomes.pop();
      observation.stop();
    }
  }

  private static void applyStaticTags(CommerceMetered annotation, Observation observation) {
    String[] tags = annotation.tags();
    if (tags.length == 0) {
      return;
    }
    if ((tags.length & 1) == 1) {
      throw new IllegalArgumentException(
          "@CommerceMetered tags must be alternating key/value pairs (got "
              + tags.length
              + " elements)");
    }
    for (int i = 0; i < tags.length; i += 2) {
      observation.lowCardinalityKeyValue(tags[i], tags[i + 1]);
    }
  }

  private static void logFailure(
      CommerceMetered annotation, ProceedingJoinPoint pjp, String name, Throwable ex) {
    if (!annotation.logFailures()) {
      return;
    }
    Level level = annotation.logFailuresAt();
    if (level == null) {
      return;
    }
    log.atLevel(level)
        .setCause(ex)
        .setMessage("metered_method_failed name={} signature={}")
        .addArgument(name)
        .addArgument(pjp.getSignature().toShortString())
        .log();
  }
}
