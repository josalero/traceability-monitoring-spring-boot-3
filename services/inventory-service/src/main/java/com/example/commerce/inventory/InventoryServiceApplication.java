package com.example.commerce.inventory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import com.example.commerce.inventory.persistence.StockEntity;
import com.example.commerce.inventory.persistence.StockRepository;
import org.springframework.context.annotation.Bean;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.example.commerce.inventory")
public class InventoryServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(InventoryServiceApplication.class, args);
  }

  @Bean
  public CommandLineRunner initData(StockRepository repository) {
    return args -> {
      if (!repository.existsById("SKU-1")) {
        repository.save(new StockEntity("SKU-1", 10));
      }
    };
  }
}
