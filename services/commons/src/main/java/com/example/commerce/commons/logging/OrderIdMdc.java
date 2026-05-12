package com.example.commerce.commons.logging;

import java.util.UUID;
import org.slf4j.MDC;

/**
 * Puts the commerce order id into {@link MDC} so every log line in scope includes {@code order_id=…}
 * from the shared Logback suffix pattern (searchable in Loki with {@code |= "order_id=<uuid>"}).
 */
public final class OrderIdMdc {

  public static final String MDC_KEY = "orderId";

  private OrderIdMdc() {}

  /**
   * Sets {@link #MDC_KEY} for the duration of {@code task}. Restores any previous value afterward.
   */
  public static void run(UUID orderId, Runnable task) {
    if (orderId == null) {
      task.run();
      return;
    }
    String previous = MDC.get(MDC_KEY);
    MDC.put(MDC_KEY, orderId.toString());
    try {
      task.run();
    } finally {
      if (previous != null) {
        MDC.put(MDC_KEY, previous);
      } else {
        MDC.remove(MDC_KEY);
      }
    }
  }
}
