package com.example.commerce.order.application;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.commerce.order.web.OrderController;
import com.example.commerce.order.web.OrderSummaryResponse;
import io.micrometer.observation.ObservationRegistry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = OrderController.class)
class OrderControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private PlaceOrderService placeOrder;

  @MockBean
  @SuppressWarnings("unused")
  private ObservationRegistry observationRegistry;

  @Test
  void list_orders_returns_json_array() throws Exception {
    UUID id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    when(placeOrder.listRecent(50)).thenReturn(List.of(
        new OrderSummaryResponse(id, "CONFIRMED", Instant.parse("2026-05-01T12:00:00Z"))));

    mockMvc.perform(get("/orders").param("limit", "50"))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", containsString("no-store")))
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].id").value(id.toString()))
        .andExpect(jsonPath("$[0].status").value("CONFIRMED"))
        .andExpect(jsonPath("$[0].createdAt").value("2026-05-01T12:00:00Z"));
  }
}
