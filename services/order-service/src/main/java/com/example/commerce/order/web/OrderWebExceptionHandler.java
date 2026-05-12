package com.example.commerce.order.web;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class OrderWebExceptionHandler {

  private final ObservationRegistry observationRegistry;

  public OrderWebExceptionHandler(ObservationRegistry observationRegistry) {
    this.observationRegistry = observationRegistry;
  }

  @ExceptionHandler(NoSuchElementException.class)
  public ProblemDetail orderNotFound(NoSuchElementException ex) {
    markHttpObservationFailure(ex, "order_not_found");
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Order not found");
    pd.setTitle("Not Found");
    return pd;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail validationFailed(MethodArgumentNotValidException ex) {
    String fields = ex.getBindingResult().getFieldErrors().stream()
        .map(FieldError::getField)
        .distinct()
        .sorted()
        .collect(Collectors.joining(","));
    Observation obs = observationRegistry.getCurrentObservation();
    if (obs != null) {
      /* Synthetic exception keeps trace status as ERROR without copying binding payload into span attributes. */
      obs.error(new IllegalArgumentException("validation_failed fields=" + fields));
      obs.highCardinalityKeyValue("commerce.http.error", "validation_failed");
      obs.highCardinalityKeyValue("commerce.validation.fields", fields);
      obs.event(Observation.Event.of("validation_failed"));
    }
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
        "One or more fields are invalid.");
    pd.setTitle("Invalid request");
    return pd;
  }

  /**
   * Marks the active HTTP server observation as failed so Micrometer Tracing records
   * {@code Span.recordException} + {@code StatusCode.ERROR} (shown as errors in Jaeger / OTLP).
   */
  private void markHttpObservationFailure(Throwable ex, String errorCode) {
    Observation obs = observationRegistry.getCurrentObservation();
    if (obs != null) {
      obs.error(ex);
      obs.highCardinalityKeyValue("commerce.http.error", errorCode);
      obs.event(Observation.Event.of(errorCode));
    }
  }
}
