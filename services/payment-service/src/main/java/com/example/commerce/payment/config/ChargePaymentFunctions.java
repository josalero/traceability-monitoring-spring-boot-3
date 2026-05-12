package com.example.commerce.payment.config;

import com.example.commerce.commons.events.InventoryReserved;
import com.example.commerce.payment.application.ChargePaymentConsumer;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChargePaymentFunctions {

  @Bean
  public Consumer<InventoryReserved> chargePayment(ChargePaymentConsumer consumer) {
    return consumer::accept;
  }
}
