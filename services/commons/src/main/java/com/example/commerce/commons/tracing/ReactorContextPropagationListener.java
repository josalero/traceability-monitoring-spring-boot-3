package com.example.commerce.commons.tracing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationListener;

/**
 * Enables Reactor {@code Hooks.enableAutomaticContextPropagation()} as early as possible so
 * Micrometer {@link io.micrometer.observation.Observation Observation} / trace context survives
 * thread hops inside Spring Cloud Stream (e.g. {@code StreamBridge} → Rabbit binder). Without this,
 * {@code traceparent} may not be written onto AMQP messages and consumers start a new trace root.
 *
 * <p>Uses reflection so services without reactor-core on the classpath (e.g. minimal utilities)
 * still load; those apps simply skip the hook.
 *
 * <p>Registered via {@code META-INF/spring/org.springframework.context.ApplicationListener}.
 */
public final class ReactorContextPropagationListener
    implements ApplicationListener<ApplicationStartingEvent> {

  private static final Logger log = LoggerFactory.getLogger(ReactorContextPropagationListener.class);

  @Override
  public void onApplicationEvent(ApplicationStartingEvent event) {
    installReactorAutomaticContextPropagationIfAvailable();
  }

  /**
   * Invokes {@code reactor.core.publisher.Hooks.enableAutomaticContextPropagation()} when reactor
   * is present; no-op otherwise.
   */
  public static void installReactorAutomaticContextPropagationIfAvailable() {
    try {
      Class<?> hooks = Class.forName("reactor.core.publisher.Hooks");
      hooks.getMethod("enableAutomaticContextPropagation").invoke(null);
      log.debug("Installed Reactor Hooks.enableAutomaticContextPropagation()");
    } catch (ClassNotFoundException ex) {
      log.debug("reactor-core not on classpath; skipping Reactor context propagation hook");
    } catch (Throwable ex) {
      log.warn(
          "Could not enable Reactor automatic context propagation; Rabbit/Stream trace linkage may be incomplete: {}",
          ex.toString());
    }
  }
}
