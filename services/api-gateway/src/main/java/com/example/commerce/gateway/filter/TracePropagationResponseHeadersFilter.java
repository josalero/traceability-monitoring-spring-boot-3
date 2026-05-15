package com.example.commerce.gateway.filter;

import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import java.util.Locale;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Echoes the active trace context on HTTP responses so browsers (and proxies) can correlate
 * requests with Jaeger / Loki. Incoming requests may also send W3C {@code traceparent} to
 * continue an existing trace — Spring WebFlux + Micrometer propagate that automatically when
 * {@code micrometer-tracing-bridge-otel} is on the classpath.
 */
@Component
class TracePropagationResponseHeadersFilter implements GlobalFilter, Ordered {

  static final String HEADER_TRACE_ID = "X-Trace-Id";
  static final String HEADER_SPAN_ID = "X-Span-Id";

  private final Tracer tracer;

  TracePropagationResponseHeadersFilter(Tracer tracer) {
    this.tracer = tracer;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    // Capture while the request is still scoped to the server span; beforeCommit runs after
    // the reactive chain may have cleared ThreadLocal / Reactor trace context.
    TraceContext ctx = tracer.currentTraceContext().context();
    if (ctx != null) {
      exchange
          .getResponse()
          .beforeCommit(
              () ->
                  Mono.fromRunnable(
                      () -> {
                        HttpHeaders headers = exchange.getResponse().getHeaders();
                        headers.set(HEADER_TRACE_ID, ctx.traceId());
                        headers.set(HEADER_SPAN_ID, ctx.spanId());
                        headers.set("traceparent", toTraceparent(ctx));
                      }));
    }
    return chain.filter(exchange);
  }

  /**
   * W3C Trace Context — allows clients to send this header on the next request to stay in the same trace.
   */
  private static String toTraceparent(TraceContext ctx) {
    String tid = padHex(ctx.traceId().toLowerCase(Locale.ROOT), 32);
    String sid = padHex(ctx.spanId().toLowerCase(Locale.ROOT), 16);
    return "00-" + tid + "-" + sid + "-01";
  }

  private static String padHex(String hex, int expectedLen) {
    if (hex.length() > expectedLen) {
      return hex.substring(hex.length() - expectedLen);
    }
    if (hex.length() < expectedLen) {
      return "0".repeat(expectedLen - hex.length()) + hex;
    }
    return hex;
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }
}
