package com.guba.router;

import com.guba.handler.UserHandler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.validation.RequestPredicate;
import io.vertx.ext.web.validation.ValidationHandler;
import io.vertx.ext.web.validation.builder.Bodies;
import io.vertx.ext.web.validation.builder.Parameters;
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder;
import io.vertx.json.schema.Draft;
import io.vertx.json.schema.JsonSchemaOptions;
import io.vertx.json.schema.SchemaRepository;

import static io.vertx.json.schema.common.dsl.Keywords.*;
import static io.vertx.json.schema.common.dsl.Schemas.*;

public class UserRouter {

  private UserRouter() {}

  public static Router create(Vertx vertx, UserHandler handler) {
    Router router = Router.router(vertx);
    // baseUri is required by Vert.x 5.x but only matters when using $ref, OpenAPI, or external schema files
    SchemaRepository schema = SchemaRepository.create(
      new JsonSchemaOptions()
        .setBaseUri("https://vertx.app")
        .setDraft(Draft.DRAFT202012)
    );

    router.get("/")
      .handler(handler::getAll);

    router.get("/:id")
      .handler(idValidation(schema))
      .handler(handler::getById);

    router.post("/")
      .handler(bodyValidation(schema))
      .handler(handler::create);

    router.put("/:id")
      .handler(idValidation(schema))
      .handler(bodyValidation(schema))
      .handler(handler::update);

    router.delete("/:id")
      .handler(idValidation(schema))
      .handler(handler::delete);

    return router;
  }

  private static ValidationHandler idValidation(SchemaRepository schema) {
    return ValidationHandlerBuilder
      .create(schema)
      .pathParameter(Parameters.param("id", intSchema().with(minimum(1))))
      .build();
  }

  private static ValidationHandler bodyValidation(SchemaRepository schema) {
    return ValidationHandlerBuilder
      .create(schema)
      .predicate(RequestPredicate.BODY_REQUIRED)
      .body(Bodies.json(
        objectSchema()
          .requiredProperty("name", stringSchema().with(minLength(1)).with(maxLength(100)))
          .requiredProperty("email", stringSchema().with(minLength(3)).with(maxLength(255)))
      ))
      .build();
  }
}
