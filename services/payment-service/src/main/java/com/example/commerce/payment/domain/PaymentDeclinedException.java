package com.example.commerce.payment.domain;

public class PaymentDeclinedException extends RuntimeException {
  public PaymentDeclinedException(String message) {
    super(message);
  }
}
