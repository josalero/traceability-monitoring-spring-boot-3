package com.example.commerce.order.web;

import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponse(UUID id, String status, Instant createdAt) {}
