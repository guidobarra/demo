package com.guba.vertx.demo;


import com.guba.vertx.demo.configs.AppConfig;
import com.guba.vertx.demo.queues.Consumer;
import com.guba.vertx.demo.routers.HealthCheckRouter;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MainApp {
    private static final Logger LOG = LoggerFactory.getLogger(MainApp.class);

    public static void main(String[] args) {
        LOG.info("Startup Main app");
        AppConfig.load("application.yml");
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new HealthCheckRouter());
        vertx.deployVerticle(new Consumer());
    }
}
