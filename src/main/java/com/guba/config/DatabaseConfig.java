package com.guba.config;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLBuilder;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

public class DatabaseConfig {

  private DatabaseConfig() {}

  public static Pool createPool(Vertx vertx, JsonObject config) {
    JsonObject db = config.getJsonObject("database");
    JsonObject poolCfg = db.getJsonObject("pool", new JsonObject());

    MySQLConnectOptions connectOptions = new MySQLConnectOptions()
      .setHost(db.getString("host"))
      .setPort(db.getInteger("port"))
      .setDatabase(db.getString("name"))
      .setUser(db.getString("user"))
      .setPassword(db.getString("password"));

    PoolOptions poolOptions = new PoolOptions()
      .setMaxSize(poolCfg.getInteger("max-size", 10));

    return MySQLBuilder.pool()
      .with(poolOptions)
      .connectingTo(connectOptions)
      .using(vertx)
      .build();
  }
}
