package com.guba.model;

import io.vertx.core.json.JsonObject;

import java.time.Instant;

public record HealthResponse(
  String status,
  String timestamp,
  String environment,
  String version,
  String serviceName
) {

  public static HealthResponse up(String environment, String version, String serviceName) {
    return new HealthResponse("UP", Instant.now().toString(), environment, version, serviceName);
  }

  public JsonObject toJson() {
    return new JsonObject()
      .put("status", status)
      .put("timestamp", timestamp)
      .put("environment", environment)
      .put("version", version)
      .put("service_name", serviceName);
  }
}
