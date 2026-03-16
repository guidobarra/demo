package com.guba.router;

import com.guba.model.HealthResponse;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class HealthCheckRouter {

  private HealthCheckRouter() {}

  public static Router create(Vertx vertx, JsonObject config) {
    String profile = System.getenv().get("VERTX_PROFILE");
    String version = config.getString("version");
    String serviceName = config.getString("name");

    Router router = Router.router(vertx);

    router.get("/").handler(ctx ->
      ctx.response()
        .putHeader("content-type", "application/json")
        .end(HealthResponse.up(profile, version, serviceName).toJson().encode())
    );

    return router;
  }
}
