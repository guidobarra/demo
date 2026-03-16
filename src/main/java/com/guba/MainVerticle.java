package com.guba;

import com.guba.config.AppConfig;
import com.guba.config.DatabaseConfig;
import com.guba.handler.UserHandler;
import com.guba.repository.UserRepository;
import com.guba.router.HealthCheckRouter;
import com.guba.router.UserRouter;
import com.guba.service.UserService;
import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.validation.BadRequestException;
import io.vertx.sqlclient.Pool;

public class MainVerticle extends VerticleBase {

  private static final Logger LOG = LoggerFactory.getLogger(MainVerticle.class);

  @Override
  public Future<?> start() {
    return AppConfig.load(vertx)
      .compose(this::bootstrap);
  }

  private Future<?> bootstrap(JsonObject config) {
    Pool pool = DatabaseConfig.createPool(vertx, config);

    UserRepository userRepository = new UserRepository(pool);
    UserService userService = new UserService(userRepository);
    UserHandler userHandler = new UserHandler(userService);

    Router router = buildRouter(userHandler, config);

    int port = config.getJsonObject("server").getInteger("port");

    return vertx.createHttpServer()
      .requestHandler(router)
      .listen(port)
      .onSuccess(http -> LOG.info("HTTP server started on port {} in {}ms", http.actualPort(), ManagementFactory.getRuntimeMXBean().getUptime()));
  }

  private Router buildRouter(UserHandler userHandler, JsonObject config) {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    router.route("/api/users/*").subRouter(UserRouter.create(vertx, userHandler));
    router.route("/health-check/*").subRouter(HealthCheckRouter.create(vertx, config));

    router.errorHandler(400, ctx -> {
      JsonObject error = new JsonObject().put("error", "Bad request");
      if (ctx.failure() instanceof BadRequestException bre) {
        error.put("details", bre.toJson());
      }
      ctx.response()
        .setStatusCode(400)
        .putHeader("content-type", "application/json")
        .end(error.encode());
    });

    return router;
  }
}
