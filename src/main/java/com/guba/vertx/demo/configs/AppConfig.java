package com.guba.vertx.demo.configs;

import io.vertx.core.json.JsonObject;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class AppConfig {

    private static JsonObject config;

    private AppConfig() {
    }

    public static void load(String resourcePath) {
        Yaml yaml = new Yaml();
        InputStream input = AppConfig.class.getClassLoader().getResourceAsStream(resourcePath);

        if (input == null) {
            throw new IllegalArgumentException("Archivo de configuración no encontrado: " + resourcePath);
        }

        Map<String, Object> map = yaml.load(input);
        config = new JsonObject(map);
    }

    public static JsonObject get() {
        if (config == null) {
            throw new IllegalStateException("AppConfig no ha sido cargado. Llamá primero a load()");
        }
        return config;
    }

    public static JsonObject section(String key) {
        return get().getJsonObject(key);
    }
}
