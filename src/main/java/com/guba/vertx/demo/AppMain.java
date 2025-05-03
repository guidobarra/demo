package com.guba.vertx.demo;

import com.guba.vertx.demo.web.routers.RouterPeople;
import io.vertx.core.Vertx;

public class AppMain {

  public static void main(String[] args) {

    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new RouterPeople());
  }
}
