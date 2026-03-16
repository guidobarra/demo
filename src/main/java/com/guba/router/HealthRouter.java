package com.guba.router;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class HealthRouter {

  private HealthRouter() {}

  public static Router create(Vertx vertx) {
    Router router = Router.router(vertx);

    router.get("/").handler(ctx ->
      ctx.response()
        .putHeader("content-type", "application/json")
        .end(new JsonObject().put("status", "UP").encode())
    );

    return router;
  }
}
