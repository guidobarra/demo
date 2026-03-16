package com.guba.config;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class AppConfig {

  private static final Logger LOG = LoggerFactory.getLogger(AppConfig.class);
  private static final String ENV_PREFIX = "VERTX_";

  private AppConfig() {}

  public static Future<JsonObject> load(Vertx vertx) {
    String profile = System.getenv().getOrDefault("VERTX_PROFILE", "dev");

    ConfigStoreOptions defaultFile = new ConfigStoreOptions()
      .setType("file")
      .setFormat("yaml")
      .setConfig(new JsonObject().put("path", "config/application.yaml"));

    ConfigStoreOptions profileFile = new ConfigStoreOptions()
      .setType("file")
      .setFormat("yaml")
      .setOptional(true)
      .setConfig(new JsonObject().put("path", "config/application-" + profile + ".yaml"));

    ConfigStoreOptions sysProps = new ConfigStoreOptions()
      .setType("sys");

    ConfigRetrieverOptions options = new ConfigRetrieverOptions()
      .addStore(defaultFile)
      .addStore(profileFile)
      .addStore(sysProps);

    ConfigRetriever retriever = ConfigRetriever.create(vertx, options);

    return retriever.getConfig()
      .map(AppConfig::applyEnvOverrides)
      .onSuccess(cfg -> LOG.info("Config loaded [profile={}]", profile));
  }

  /**
   * Any env var starting with VERTX_ is mapped into the nested config.
   * Underscore (_) separates nesting levels, hyphens (-) are kept as-is.
   *
   * VERTX_DATABASE_HOST           -> database.host
   * VERTX_DATABASE_PASSWORD       -> database.password
   * VERTX_DATABASE_POOL_MAX-SIZE  -> database.pool.max-size
   * VERTX_SERVER_PORT             -> server.port
   */
  private static JsonObject applyEnvOverrides(JsonObject config) {
    for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
      String key = entry.getKey();
      if (!key.startsWith(ENV_PREFIX) || key.equals("VERTX_PROFILE")) continue;

      String path = key.substring(ENV_PREFIX.length()).toLowerCase().replace("_", ".");
      setNestedValue(config, path, entry.getValue());
    }
    return config;
  }

  private static void setNestedValue(JsonObject config, String path, String value) {
    String[] parts = path.split("\\.");
    JsonObject target = config;

    for (int i = 0; i < parts.length - 1; i++) {
      JsonObject child = target.getJsonObject(parts[i]);
      if (child == null) {
        child = new JsonObject();
        target.put(parts[i], child);
      }
      target = child;
    }

    String leaf = parts[parts.length - 1];
    Object existing = target.getValue(leaf);

    switch (existing) {
      case Integer i -> target.put(leaf, Integer.parseInt(value));
      case Long l -> target.put(leaf, Long.parseLong(value));
      case Boolean b -> target.put(leaf, Boolean.parseBoolean(value));
      case null, default -> target.put(leaf, value);
    }
  }
}
