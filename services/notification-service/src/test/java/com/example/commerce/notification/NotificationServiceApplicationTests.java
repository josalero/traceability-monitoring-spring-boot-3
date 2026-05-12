package com.example.commerce.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
  "spring.cloud.config.enabled=false",
  "spring.config.import=",
  "eureka.client.enabled=false"
})
@AutoConfigureObservability
class NotificationServiceApplicationTests {
  @Test
  void contextLoads() {
  }
}
