package com.example.commerce.commons.amqp;

public final class EventChannels {
  public static final String OUTPUT_BINDING       = "commerce-events-out";
  public static final String ROUTING_KEY_HEADER   = "eventRoutingKey";
  private EventChannels() {}
}
