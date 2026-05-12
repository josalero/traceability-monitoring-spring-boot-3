package com.example.commerce.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
  "spring.profiles.active=native",
  "spring.cloud.config.server.native.search-locations=classpath:/",
  "spring.cloud.config.server.native.add-label-locations=false"
})
class ConfigServerApplicationTests {
    @Test
    void contextLoads() {
    }
}
