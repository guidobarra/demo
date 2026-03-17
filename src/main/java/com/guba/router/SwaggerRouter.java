package com.guba.router;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

public class SwaggerRouter {

  private static final String SWAGGER_UI_PATH = "META-INF/resources/webjars/swagger-ui/5.30.1";

  private SwaggerRouter() {}

  public static Router create(Vertx vertx) {
    Router router = Router.router(vertx);

    router.get("/openapi.yaml").handler(ctx ->
      vertx.fileSystem().readFile("webroot/openapi.yaml")
        .onSuccess(buffer -> ctx.response()
          .putHeader("content-type", "application/yaml")
          .end(buffer))
        .onFailure(err -> ctx.response().setStatusCode(404).end())
    );

    router.get("/").handler(ctx ->
      ctx.response()
        .putHeader("content-type", "text/html")
        .end(indexHtml())
    );

    router.route("/*").handler(StaticHandler.create(SWAGGER_UI_PATH));

    return router;
  }

  private static String indexHtml() {
    return """
      <!DOCTYPE html>
      <html lang="en">
      <head>
        <meta charset="UTF-8">
        <title>API Docs</title>
        <link rel="stylesheet" href="/swagger/swagger-ui.css">
      </head>
      <body>
        <div id="swagger-ui"></div>
        <script src="/swagger/swagger-ui-bundle.js"></script>
        <script>
          SwaggerUIBundle({
            url: '/swagger/openapi.yaml',
            dom_id: '#swagger-ui',
            presets: [SwaggerUIBundle.presets.apis],
            layout: 'BaseLayout'
          });
        </script>
      </body>
      </html>
      """;
  }
}
