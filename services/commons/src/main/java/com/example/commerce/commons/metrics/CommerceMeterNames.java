package com.example.commerce.commons.metrics;

/**
 * Builds Micrometer / Observation names that embed the deployed service id (from {@code
 * spring.application.name}), so Prometheus series are self-describing even outside aggregated
 * dashboards. Hyphens in the service name are normalized to underscores because Micrometer meter
 * names are dot-segmented identifiers.
 *
 * <p>For {@code spring.application.name=order-service}, {@link #prefix(String) prefix("order.detail.cache")}
 * returns {@code commerce.order_service.order.detail.cache}, which Micrometer's Prometheus registry
 * exposes as {@code commerce_order_service_order_detail_cache_total} for a counter.
 */
public final class CommerceMeterNames {

  private final String serviceSlug;

  private CommerceMeterNames(String serviceSlug) {
    this.serviceSlug = serviceSlug;
  }

  public static CommerceMeterNames fromSpringApplicationName(String applicationName) {
    return new CommerceMeterNames(sanitize(applicationName));
  }

  static String sanitize(String applicationName) {
    if (applicationName == null || applicationName.isBlank()) {
      return "unknown_service";
    }
    return applicationName.trim().replace('-', '_');
  }

  /** Returns the normalized service segment used inside meter names (e.g. {@code order_service}). */
  public String serviceSlug() {
    return serviceSlug;
  }

  /** Prefixes {@code suffix} with {@code commerce.{service}.}; suffix may already contain dots. */
  public String prefix(String suffix) {
    if (suffix == null || suffix.isBlank()) {
      throw new IllegalArgumentException("Meter name suffix must not be blank");
    }
    return "commerce." + serviceSlug + "." + suffix;
  }

  /** Joins multiple segments with dots before prefixing — convenience for varargs callers. */
  public String prefix(String first, String... more) {
    StringBuilder sb = new StringBuilder(first);
    for (String segment : more) {
      sb.append('.').append(segment);
    }
    return prefix(sb.toString());
  }
}
