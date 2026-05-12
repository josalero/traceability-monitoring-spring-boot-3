package com.example.commerce.commons.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.Observation;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Inline counter + observation-tag helper for code wrapped in {@link CommerceMetered}. Records an
 * outcome on the current innermost method's observation scope so the developer chooses whether to
 * increment (and with what tag value) — the annotation never auto-increments.
 *
 * <p>Nesting is supported: when a {@link CommerceMetered} method calls another instrumented bean,
 * outcomes and tags apply to the inner scope until it completes, then the outer scope resumes.
 *
 * <p>Calls made outside of a {@link CommerceMetered}-instrumented method are no-ops, so library
 * code may call this safely without forcing every caller to be annotated.
 */
public final class MeteredOutcomes {

  private static final ThreadLocal<Deque<Context>> STACK = new ThreadLocal<>();

  private MeteredOutcomes() {}

  /** Records an outcome with the default tag key {@code result} (matches existing dashboards). */
  public static void outcome(String value) {
    outcome("result", value);
  }

  /**
   * Records an outcome on the active observation and increments the matching counter. No-op if the
   * call is made outside of a {@link CommerceMetered} scope.
   */
  public static void outcome(String tagKey, String tagValue) {
    if (tagKey == null || tagKey.isBlank() || tagValue == null || tagValue.isBlank()) {
      throw new IllegalArgumentException("Outcome tagKey and tagValue must be non-blank");
    }
    Context ctx = peek();
    if (ctx == null) {
      return;
    }
    ctx.observation().lowCardinalityKeyValue(tagKey, tagValue);
    ctx.meterRegistry().counter(ctx.metricName(), tagKey, tagValue).increment();
  }

  /** Adds a low-cardinality tag to the innermost active observation. No-op outside a scope. */
  public static void lowCardinalityTag(String key, String value) {
    if (key == null || key.isBlank() || value == null) {
      throw new IllegalArgumentException("Tag key must be non-blank and value must not be null");
    }
    Context ctx = peek();
    if (ctx == null) {
      return;
    }
    ctx.observation().lowCardinalityKeyValue(key, value);
  }

  /** Adds a high-cardinality tag to the innermost active observation. No-op outside a scope. */
  public static void highCardinalityTag(String key, String value) {
    if (key == null || key.isBlank() || value == null) {
      throw new IllegalArgumentException("Tag key must be non-blank and value must not be null");
    }
    Context ctx = peek();
    if (ctx == null) {
      return;
    }
    ctx.observation().highCardinalityKeyValue(key, value);
  }

  /** Records an observation event on the innermost active observation. No-op outside a scope. */
  public static void event(String eventName) {
    if (eventName == null || eventName.isBlank()) {
      throw new IllegalArgumentException("eventName must not be blank");
    }
    Context ctx = peek();
    if (ctx == null) {
      return;
    }
    ctx.observation().event(Observation.Event.of(eventName));
  }

  /**
   * Records an error on the innermost active observation (for example so {@code
   * commerce.observation.errors} increments) without throwing. No-op outside a scope.
   */
  public static void error(Throwable throwable) {
    if (throwable == null) {
      throw new IllegalArgumentException("throwable must not be null");
    }
    Context ctx = peek();
    if (ctx == null) {
      return;
    }
    ctx.observation().error(throwable);
  }

  static void push(Context context) {
    Deque<Context> deque = STACK.get();
    if (deque == null) {
      deque = new ArrayDeque<>(4);
      STACK.set(deque);
    }
    deque.push(context);
  }

  static void pop() {
    Deque<Context> deque = STACK.get();
    if (deque == null || deque.isEmpty()) {
      return;
    }
    deque.pop();
    if (deque.isEmpty()) {
      STACK.remove();
    }
  }

  private static Context peek() {
    Deque<Context> deque = STACK.get();
    if (deque == null || deque.isEmpty()) {
      return null;
    }
    return deque.peek();
  }

  record Context(String metricName, MeterRegistry meterRegistry, Observation observation) {}
}
