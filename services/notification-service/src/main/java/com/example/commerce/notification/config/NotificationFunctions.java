package com.example.commerce.notification.config;

import com.example.commerce.commons.events.InventoryFailed;
import com.example.commerce.commons.events.PaymentCompleted;
import com.example.commerce.commons.events.PaymentFailed;
import com.example.commerce.notification.application.NotificationDispatchConsumer;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationFunctions {

  @Bean
  public Consumer<PaymentCompleted> onPaymentCompleted(NotificationDispatchConsumer consumer) {
    return consumer::onPaymentCompleted;
  }

  @Bean
  public Consumer<PaymentFailed> onPaymentFailed(NotificationDispatchConsumer consumer) {
    return consumer::onPaymentFailed;
  }

  @Bean
  public Consumer<InventoryFailed> onInventoryFailed(NotificationDispatchConsumer consumer) {
    return consumer::onInventoryFailed;
  }
}
