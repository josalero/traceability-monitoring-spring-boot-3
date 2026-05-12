package com.example.commerce.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
  "spring.cloud.config.enabled=false",
  "spring.config.import=",
  "eureka.client.enabled=false"
})
class ApiGatewayApplicationTests {
    @Test
    void contextLoads() {
    }
}
