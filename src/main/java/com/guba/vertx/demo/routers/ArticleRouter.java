package com.guba.vertx.demo.routers;

import com.guba.vertx.demo.configs.AppConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ArticleRouter extends AbstractVerticle {

    private static final String PATH_ARTICLES = "/articles";
    private static final String APPLICATION_JSON = "application/json";
    private static final String ERROR = "error";



    @Override
    public void start(Promise<Void> startPromise) {

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router
                .post(PATH_ARTICLES)
                .consumes(APPLICATION_JSON)
                .produces(APPLICATION_JSON)
                .handler(ctx -> {
                    //Article request = ctx.body().asJsonObject().mapTo(Article.class);
                    //Article request = ctx.body().asPojo(Article.class);

                    vertx.eventBus().<JsonObject>request("article.service.create", ctx.body().asJsonObject(), reply -> {

                        if (reply.succeeded()) {
                            JsonObject response = reply.result().body();

                            ctx.response()
                                    .setStatusCode(201)
                                    .end(response.encodePrettily());
                        } else {
                            ctx.response()
                                    .setStatusCode(500)
                                    .end(new JsonObject().put(ERROR, reply.cause().getMessage()).encodePrettily());
                        }
                    });
                });

        router
                .get(PATH_ARTICLES)
                .produces(APPLICATION_JSON)
                .handler(ctx -> vertx.eventBus().<JsonArray>request("article.service.getAll", "", reply -> {
                    if (reply.succeeded()) {
                        JsonArray response = reply.result().body();

                        ctx.response()
                                .setStatusCode(200)
                                .end(response.encodePrettily());
                    } else {
                        ctx.response()
                                .setStatusCode(500)
                                .end(new JsonObject().put(ERROR, reply.cause().getMessage()).encodePrettily());
                    }
                }));

        router
                .get(PATH_ARTICLES + "/:id")
                .produces(APPLICATION_JSON)
                .handler(ctx -> vertx.eventBus().<JsonObject>request("article.service.getById", ctx.pathParam("id"), reply -> {
                    if (reply.succeeded()) {
                        JsonObject response = reply.result().body();

                        ctx.response()
                                .setStatusCode(200)
                                .end(response.encodePrettily());
                    } else {
                        ctx.response()
                                .setStatusCode(500)
                                .end(new JsonObject().put(ERROR, reply.cause().getMessage()).encodePrettily());
                    }
                }));

        int port = AppConfig.section("http").getInteger("port", 8080);
        String host = AppConfig.section("http").getString("host", "0.0.0.0");

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(port)
                .onSuccess(server -> log.info("Server HTTP en http://" + host + ":" +port))
                .onFailure(err -> log.error("Error Startup Server: " + err.getMessage()));
    }
}
