package com.guba.vertx.demo.queues;

import com.guba.vertx.demo.configs.ActiveMQConfig;
import io.vertx.amqp.AmqpClient;
import io.vertx.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Consumer extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(Consumer.class);

    @Override
    public void start() {

        AmqpClient client = ActiveMQConfig.getClient(vertx);
        String topic = ActiveMQConfig.getTopicName();

        client
                .connect()
                .onSuccess(conn -> {
                    LOG.info("✅ Connected to AMQP broker");
                    conn
                            .createReceiver(topic)
                            .onSuccess(receiver -> receiver
                                    .handler(msg -> LOG.info("Received {} ", msg.bodyAsString()))
                                    .exceptionHandler(error -> LOG.error("❌ Failed to receiver handler: ", error)))
                            .onFailure(error -> LOG.error("❌ Failed to create receiver: ", error));

                })
                .onFailure(error -> LOG.error("❌ Connection failed: ", error));
    }
}
