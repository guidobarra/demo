package com.guba.vertx.demo.routers;

import com.guba.vertx.demo.configs.AppConfig;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HealthCheckRouter extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckRouter.class);

    @Override
    public void start(Promise<Void> startPromise) {

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router
                .get("/health-check")
                .produces(HttpHeaderValues.APPLICATION_JSON.toString())
                .handler(ctx -> ctx.response()
                        .setStatusCode(200)
                        .end(new JsonObject().put("status", "OK").encodePrettily())
                );

        int port = AppConfig.section("http").getInteger("port", 8080);
        String host = AppConfig.section("http").getString("host", "0.0.0.0");

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(port)
                .onSuccess(server -> LOG.info("Server HTTP en http://{}:{}", host, port))
                .onFailure(err -> LOG.error("Error Startup Server: {}", err.getMessage()));
    }
}
