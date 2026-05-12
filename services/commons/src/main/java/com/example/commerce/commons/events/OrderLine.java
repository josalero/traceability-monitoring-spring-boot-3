package com.example.commerce.commons.events;

public record OrderLine(String sku, int quantity, java.math.BigDecimal unitPrice) {}
