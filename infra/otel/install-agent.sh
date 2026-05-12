#!/usr/bin/env bash
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
mkdir -p "$DIR/agent"

OTEL_VERSION="2.1.0" # You can adjust as needed. 2.1.0 supports Spring Boot 3.x well.
JAR_PATH="$DIR/agent/opentelemetry-javaagent.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "Downloading OpenTelemetry Java agent v$OTEL_VERSION..."
    curl -L "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_VERSION}/opentelemetry-javaagent.jar" -o "$JAR_PATH"
else
    echo "OpenTelemetry Java agent already exists."
fi
