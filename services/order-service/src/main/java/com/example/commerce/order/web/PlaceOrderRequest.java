package com.example.commerce.order.web;

import com.example.commerce.commons.events.OrderLine;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PlaceOrderRequest(
    @Email String customerEmail,
    @NotEmpty List<OrderLine> lines
) {}
