package com.example.commerce.payment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
  "spring.cloud.config.enabled=false",
  "spring.config.import=",
  "eureka.client.enabled=false",
  "spring.datasource.url=jdbc:h2:mem:testdb",
  "spring.datasource.driver-class-name=org.h2.Driver",
  "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureObservability
class PaymentServiceApplicationTests {
  @Test
  void contextLoads() {
  }
}
