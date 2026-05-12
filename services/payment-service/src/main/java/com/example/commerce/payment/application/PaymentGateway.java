package com.example.commerce.payment.application;

import com.example.commerce.payment.domain.PaymentDeclinedException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PaymentGateway {
  public String charge(UUID orderId) {
    // Simulated payment logic
    if (Math.random() < 0.1) {
      throw new PaymentDeclinedException("Card declined for order " + orderId);
    }
    return "txn_" + UUID.randomUUID().toString().substring(0, 8);
  }
}
