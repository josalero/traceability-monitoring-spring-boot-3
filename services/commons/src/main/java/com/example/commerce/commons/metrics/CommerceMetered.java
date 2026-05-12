package com.example.commerce.commons.metrics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.slf4j.event.Level;

/**
 * Wraps the annotated method in an {@link io.micrometer.observation.Observation Observation} named
 * {@code commerce.{service}.{value}} (service comes from {@code spring.application.name} via
 * {@link CommerceMeterNames}). Inside the method, code may emit outcome counters with the same
 * base name via {@link MeteredOutcomes#outcome(String)} — the annotation does not auto-increment a
 * default counter, so "calculate an increment or not" is just a matter of calling, or not calling,
 * the helper.
 *
 * <p>Exceptions thrown by the method propagate; they are also recorded on the Observation
 * (driving the global {@code commerce.observation.errors} counter). When {@link #logFailures()}
 * is {@code true} (default), failures are also logged at {@link #logFailuresAt()} (default
 * {@link Level#WARN WARN}). Set {@code logFailures = false} for expected failures (for example HTTP 404)
 * so observations still record errors without aspect WARN noise.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommerceMetered {

  /**
   * Dotted suffix appended to {@code commerce.{service}.}. Required; e.g. {@code "order.detail.cache"}.
   */
  String value();

  /**
   * Static low-cardinality tags applied to the Observation as alternating key/value pairs, e.g.
   * {@code {"feature", "checkout"}}. An odd-length array fails fast at startup of the aspect.
   */
  String[] tags() default {};

  /**
   * When {@code false}, the aspect does not log thrown exceptions (Observation error recording is
   * unchanged). Use for expected domain outcomes mapped to client errors.
   */
  boolean logFailures() default true;

  /** Log level used when {@link #logFailures()} is {@code true} and the method throws. */
  Level logFailuresAt() default Level.WARN;
}
