package com.example.commerce.commons.metrics;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MeteredOutcomesTest {

  @Test
  void outcome_rejects_blank_tag_value() {
    assertThatThrownBy(() -> MeteredOutcomes.outcome("result", ""))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> MeteredOutcomes.outcome(" "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void tag_helpers_reject_invalid_arguments() {
    assertThatThrownBy(() -> MeteredOutcomes.lowCardinalityTag("", "v"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> MeteredOutcomes.highCardinalityTag("k", null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> MeteredOutcomes.event("  "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void error_rejects_null_throwable() {
    assertThatThrownBy(() -> MeteredOutcomes.error(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void helpers_outside_scope_do_not_throw() {
    assertThatCode(() -> MeteredOutcomes.lowCardinalityTag("k", "v")).doesNotThrowAnyException();
    assertThatCode(() -> MeteredOutcomes.highCardinalityTag("k", "v")).doesNotThrowAnyException();
    assertThatCode(() -> MeteredOutcomes.event("x")).doesNotThrowAnyException();
    assertThatCode(() -> MeteredOutcomes.error(new RuntimeException("boom")))
        .doesNotThrowAnyException();
  }
}
