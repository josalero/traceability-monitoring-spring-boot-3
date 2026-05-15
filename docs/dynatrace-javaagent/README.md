# Dynatrace observability options

This repository can be explained through four valid Dynatrace integration patterns. They solve similar problems, but they optimize for different things.

| Option | Best when | Main tradeoff |
|---|---|---|
| [1. OneAgent](oneagent.md) | You want the most Dynatrace-native experience with the least application-level telemetry wiring. | Strongest Dynatrace coupling; requires OneAgent deployment and runtime access. |
| [2. OpenTelemetry Java agent + Dynatrace direct export](otel-java-agent-direct-dynatrace.md) | You want strong automatic instrumentation with fewer moving parts than a collector pipeline. | Less flexible than a centralized collector. |
| [3. OpenTelemetry Java agent + Collector + Dynatrace](otel-java-agent-collector-dynatrace.md) | You want rich auto-instrumentation plus a vendor-neutral telemetry pipeline with central processing. | More infrastructure to run and maintain. |
| [4. Agentless custom OpenTelemetry SDK + Dynatrace](custom-sdk-dynatrace.md) | You want no runtime agent and are willing to own instrumentation in the application. | Highest engineering ownership and the easiest path to trace-coverage gaps. |

## Quick comparison

| Dimension | OneAgent | OTel Java agent direct | OTel Java agent + Collector | Custom SDK, no agent |
|---|---:|---:|---:|---:|
| Runtime agent required | Yes | Yes | Yes | No |
| Collector required | No | No | Yes | No |
| Out-of-the-box instrumentation breadth | Highest | High | High | Lowest unless you add it |
| Dynatrace-native topology/enrichment | Highest | Medium | Medium | Medium |
| Vendor portability | Low | Medium | High | High |
| App code changes | Low | Low | Low | Medium / high |
| Infrastructure to maintain | Medium | Low | Highest | Lowest |
| Fit for this Spring Boot 3.0.9 repo | Good | **Current implementation** | Good | Possible, but most work |

## How I would choose

### Choose OneAgent when
- the organization is already standardized on Dynatrace,
- you want the fastest path to broad visibility,
- and portability is less important than low app-side effort.

### Choose OTel Java agent direct export when
- Dynatrace is your only trace backend,
- you want fewer moving parts than a collector pipeline,
- and you still want broad automatic instrumentation.

### Choose OTel Java agent + Collector when
- you want OpenTelemetry as the common telemetry contract,
- you may fan out to multiple backends later,
- or you need central processing such as redaction, batching, sampling, or enrichment.

### Choose custom SDK when
- avoiding any runtime agent is a hard requirement,
- you accept narrower automatic coverage,
- and your team is willing to own instrumentation quality in code.

## Recommendation for this repository

For **minimum friction with OpenTelemetry**, use **OpenTelemetry Java agent direct export** — that is the path currently implemented in this repository.

Use **OneAgent** when Dynatrace-native depth matters more than portability. Use **OpenTelemetry Java agent + Collector** when you need centralized processing or future backend flexibility. Use the **custom SDK** path only after deciding exactly which spans must exist for the business flow and validating that the agentless path preserves them.
