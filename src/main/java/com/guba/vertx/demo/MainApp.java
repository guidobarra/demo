package com.guba.vertx.demo;


import com.guba.vertx.demo.configs.AppConfig;
import com.guba.vertx.demo.routers.ArticleRouter;
import com.guba.vertx.demo.services.ArticleService;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainApp {

    public static void main(String[] args) {
        log.info("Startup Main app");
        AppConfig.load("application.yml");
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new ArticleRouter());
        vertx.deployVerticle(new ArticleService());
    }
}
