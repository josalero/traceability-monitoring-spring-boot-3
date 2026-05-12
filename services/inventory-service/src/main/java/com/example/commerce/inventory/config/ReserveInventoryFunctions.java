package com.example.commerce.inventory.config;

import com.example.commerce.commons.events.OrderCreated;
import com.example.commerce.inventory.application.ReserveOrderCreatedConsumer;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReserveInventoryFunctions {

  @Bean
  public Consumer<OrderCreated> reserveInventory(ReserveOrderCreatedConsumer consumer) {
    return consumer::accept;
  }
}
