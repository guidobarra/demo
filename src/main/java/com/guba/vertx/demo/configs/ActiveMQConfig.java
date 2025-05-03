package com.guba.vertx.demo.configs;

import io.vertx.amqp.AmqpClient;
import io.vertx.amqp.AmqpClientOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class ActiveMQConfig {

    private ActiveMQConfig() {}

    public static AmqpClient getClient(Vertx vertx) {
        JsonObject properties = AppConfig.section("activemq");
        String broker = properties.getString("broker");
        Integer port = properties.getInteger("port");
        String user = properties.getString("user");
        String pass = properties.getString("password");

        AmqpClientOptions options = new AmqpClientOptions()
                .setHost(broker)
                .setPort(port)
                .setUsername(user)
                .setPassword(pass);

        return AmqpClient.create(vertx, options);
    }

    public static String getTopicName() {
        return AppConfig.section("activemq").getString("topic");
    }
}
