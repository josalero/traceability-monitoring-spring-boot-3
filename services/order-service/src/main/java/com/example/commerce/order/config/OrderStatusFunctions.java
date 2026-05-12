package com.example.commerce.order.config;

import com.example.commerce.commons.events.InventoryFailed;
import com.example.commerce.commons.events.PaymentCompleted;
import com.example.commerce.commons.events.PaymentFailed;
import com.example.commerce.order.application.OrderStatusConsumer;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderStatusFunctions {

  @Bean
  public Consumer<PaymentCompleted> onPaymentCompleted(OrderStatusConsumer consumer) {
    return consumer::onPaymentCompleted;
  }

  @Bean
  public Consumer<PaymentFailed> onPaymentFailed(OrderStatusConsumer consumer) {
    return consumer::onPaymentFailed;
  }

  @Bean
  public Consumer<InventoryFailed> onInventoryFailed(OrderStatusConsumer consumer) {
    return consumer::onInventoryFailed;
  }
}
