package com.guba.handler;

import com.guba.service.UserService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;

public class UserHandler {

  private final UserService service;

  public UserHandler(UserService service) {
    this.service = service;
  }

  public void getAll(RoutingContext ctx) {
    service.findAll()
      .onSuccess(users -> {
        JsonArray arr = new JsonArray();
        users.forEach(u -> arr.add(u.toJson()));
        json(ctx, 200, arr);
      })
      .onFailure(err -> error(ctx, 500, err.getMessage()));
  }

  public void getById(RoutingContext ctx) {
    long id = params(ctx).pathParameter("id").getLong();

    service.findById(id)
      .onSuccess(opt -> opt
        .map(user -> json(ctx, 200, user.toJson()))
        .orElseGet(() -> error(ctx, 404, "User not found")))
      .onFailure(err -> error(ctx, 500, err.getMessage()));
  }

  public void create(RoutingContext ctx) {
    JsonObject body = params(ctx).body().getJsonObject();

    service.create(body.getString("name"), body.getString("email"))
      .onSuccess(user -> json(ctx, 201, user.toJson()))
      .onFailure(err -> handleServiceError(ctx, err));
  }

  public void update(RoutingContext ctx) {
    long id = params(ctx).pathParameter("id").getLong();
    JsonObject body = params(ctx).body().getJsonObject();

    service.update(id, body.getString("name"), body.getString("email"))
      .onSuccess(opt -> opt
        .map(user -> json(ctx, 200, user.toJson()))
        .orElseGet(() -> error(ctx, 404, "User not found")))
      .onFailure(err -> handleServiceError(ctx, err));
  }

  public void delete(RoutingContext ctx) {
    long id = params(ctx).pathParameter("id").getLong();

    service.delete(id)
      .onSuccess(deleted -> {
        if (!deleted) {
          error(ctx, 404, "User not found");
          return;
        }
        ctx.response().setStatusCode(204).end();
      })
      .onFailure(err -> error(ctx, 500, err.getMessage()));
  }

  private RequestParameters params(RoutingContext ctx) {
    return ctx.get(ValidationHandler.REQUEST_CONTEXT_KEY);
  }

  private void handleServiceError(RoutingContext ctx, Throwable err) {
    if (err.getMessage() != null && err.getMessage().contains("Duplicate entry")) {
      error(ctx, 409, "A user with that email already exists");
    } else {
      error(ctx, 500, err.getMessage());
    }
  }

  private Void json(RoutingContext ctx, int status, Object body) {
    ctx.response()
      .setStatusCode(status)
      .putHeader("content-type", "application/json")
      .end(body.toString());
    return null;
  }

  private Void error(RoutingContext ctx, int status, String message) {
    ctx.response()
      .setStatusCode(status)
      .putHeader("content-type", "application/json")
      .end(new JsonObject().put("error", message).encode());
    return null;
  }
}
