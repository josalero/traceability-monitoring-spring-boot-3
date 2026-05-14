package com.example.commerce.commons.tracing;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class ReactorContextPropagationListenerTest {

  @Test
  void installReactorAutomaticContextPropagationIfAvailable_doesNotThrow() {
    assertThatCode(ReactorContextPropagationListener::installReactorAutomaticContextPropagationIfAvailable)
        .doesNotThrowAnyException();
  }
}
