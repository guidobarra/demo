package com.guba.vertx.demo.web.routers;

import com.guba.vertx.demo.services.PersonService;
import com.guba.vertx.demo.web.model.Person;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

public class RouterPeople extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(RouterPeople.class);
  private static final String PEOPLE = "/people";
  private static final String PEOPLE_DNI = "/people/:dni";

  private final PersonService personService = new PersonService();

  @Override
  public void start() {
    Router router = Router.router(vertx);

    router.route().handler(BodyHandler.create());

    router
      .get(PEOPLE)
      .handler(ctx ->
        personService.all()
          .onSuccess(people -> jsonResponse(
            ctx,
            new JsonArray(people.stream().map(JsonObject::mapFrom).collect(Collectors.toList())),
            200))
          .onFailure(err -> errorResponse(ctx, 500, err.getMessage()))
      );

    router
      .post(PEOPLE)
      .handler(ctx -> {
        Person person = ctx.body().asJsonObject().mapTo(Person.class);

        personService.create(person)
          .onSuccess(p -> jsonResponse(ctx, JsonObject.mapFrom(p), 201))
          .onFailure(err -> errorResponse(ctx, 500, err.getMessage()));
      });

    router
      .get(PEOPLE_DNI)
      .handler(ctx -> {
        String dni = ctx.pathParam("dni");

        personService.findByDni(Integer.valueOf(dni))
          .onSuccess(p -> jsonResponse(ctx, JsonObject.mapFrom(p), 200))
          .onFailure(err -> errorResponse(ctx, 404, err.getMessage()));
      });

    router
      .put(PEOPLE_DNI)
      .handler(ctx -> {
        String dni = ctx.pathParam("dni");
        Person updatePerson = ctx.body().asJsonObject().mapTo(Person.class);

        personService.update(Integer.valueOf(dni), updatePerson)
          .onSuccess(p -> jsonResponse(ctx, JsonObject.mapFrom(p), 200))
          .onFailure(err -> errorResponse(ctx, 404, err.getMessage()));
      });

    router
      .delete(PEOPLE_DNI)
      .handler(ctx -> {
        String dni = ctx.pathParam("dni");

        personService.delete(Integer.valueOf(dni))
          .onSuccess(v -> ctx.response().setStatusCode(204).end())
          .onFailure(err -> errorResponse(ctx, 404, err.getMessage()));
      });

    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8080)
      .onSuccess(server -> logger.info("Server HTTP en http://localhost:8080"))
      .onFailure(err -> logger.error("Error Startup Server: {}", err.getMessage()));
  }

  private void jsonResponse(RoutingContext ctx, Object body, int statusCode) {
    String payload;
    if (body instanceof JsonObject jo) {
      payload = jo.encode();
    } else if (body instanceof JsonArray ja) {
      payload = ja.encode();
    } else {
      payload = JsonObject.mapFrom(body).encode();
    }
    ctx
      .response()
      .putHeader(CONTENT_TYPE, "application/json")
      .setStatusCode(statusCode)
      .end(payload);
  }

  private void errorResponse(RoutingContext ctx, int statusCode, String message) {
    ctx.response()
      .putHeader(CONTENT_TYPE, "application/json")
      .setStatusCode(statusCode)
      .end(new JsonObject().put("error", message).encode());
  }
}
