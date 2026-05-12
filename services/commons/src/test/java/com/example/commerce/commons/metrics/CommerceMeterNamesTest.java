package com.example.commerce.commons.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CommerceMeterNamesTest {

  @Test
  void sanitize_maps_hyphens_to_underscores() {
    assertThat(CommerceMeterNames.sanitize("order-service")).isEqualTo("order_service");
    assertThat(CommerceMeterNames.sanitize("inventory-service")).isEqualTo("inventory_service");
  }

  @Test
  void sanitize_falls_back_when_application_name_is_missing() {
    assertThat(CommerceMeterNames.sanitize(null)).isEqualTo("unknown_service");
    assertThat(CommerceMeterNames.sanitize("   ")).isEqualTo("unknown_service");
  }

  @Test
  void prefix_includes_service_slug() {
    CommerceMeterNames names = CommerceMeterNames.fromSpringApplicationName("order-service");
    assertThat(names.prefix("order.detail.cache"))
        .isEqualTo("commerce.order_service.order.detail.cache");
    assertThat(names.prefix("order", "detail", "cache"))
        .isEqualTo("commerce.order_service.order.detail.cache");
    assertThat(names.serviceSlug()).isEqualTo("order_service");
  }

  @Test
  void prefix_rejects_blank_suffix() {
    CommerceMeterNames names = CommerceMeterNames.fromSpringApplicationName("order-service");
    assertThatThrownBy(() -> names.prefix("  ")).isInstanceOf(IllegalArgumentException.class);
  }
}
