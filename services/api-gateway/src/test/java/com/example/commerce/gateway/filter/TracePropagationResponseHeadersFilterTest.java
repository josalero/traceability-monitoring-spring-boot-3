package com.example.commerce.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class TracePropagationResponseHeadersFilterTest {

  @Test
  void beforeCommitSetsHeadersFromTraceContextCapturedAtFilterEntry() {
    TraceContext ctx = mock(TraceContext.class);
    when(ctx.traceId()).thenReturn("aabbccddeeff00112233445566778899");
    when(ctx.spanId()).thenReturn("1122334455667788");

    CurrentTraceContext current = mock(CurrentTraceContext.class);
    when(current.context()).thenReturn(ctx);

    Tracer tracer = mock(Tracer.class);
    when(tracer.currentTraceContext()).thenReturn(current);

    HttpHeaders headers = new HttpHeaders();
    ServerHttpResponse response = mock(ServerHttpResponse.class);
    when(response.getHeaders()).thenReturn(headers);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Supplier<Mono<Void>>> captor = ArgumentCaptor.forClass(Supplier.class);

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    when(exchange.getResponse()).thenReturn(response);

    doNothing().when(response).beforeCommit(captor.capture());

    GatewayFilterChain chain = e -> Mono.empty();

    new TracePropagationResponseHeadersFilter(tracer).filter(exchange, chain).block();

    assertThat(captor.getValue()).isNotNull();
    captor.getValue().get().block();

    assertThat(headers.getFirst(TracePropagationResponseHeadersFilter.HEADER_TRACE_ID))
        .isEqualTo("aabbccddeeff00112233445566778899");
    assertThat(headers.getFirst(TracePropagationResponseHeadersFilter.HEADER_SPAN_ID))
        .isEqualTo("1122334455667788");
    assertThat(headers.getFirst("traceparent"))
        .isEqualTo("00-aabbccddeeff00112233445566778899-1122334455667788-01");
  }

  @Test
  void whenNoTraceContextBeforeCommitIsNotRegistered() {
    CurrentTraceContext current = mock(CurrentTraceContext.class);
    when(current.context()).thenReturn(null);

    Tracer tracer = mock(Tracer.class);
    when(tracer.currentTraceContext()).thenReturn(current);

    ServerWebExchange exchange = mock(ServerWebExchange.class);

    GatewayFilterChain chain = e -> Mono.empty();

    new TracePropagationResponseHeadersFilter(tracer).filter(exchange, chain).block();

    verify(exchange, never()).getResponse();
  }
}
