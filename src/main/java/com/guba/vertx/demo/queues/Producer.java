package com.guba.vertx.demo.queues;

import com.guba.vertx.demo.configs.ActiveMQConfig;
import io.vertx.amqp.AmqpClient;
import io.vertx.amqp.AmqpMessage;
import io.vertx.core.AbstractVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

public class Producer extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(Producer.class);

    @Override
    public void start() {
        AmqpClient client = ActiveMQConfig.getClient(vertx);
        String topic = ActiveMQConfig.getTopicName();

        client
                .connect()
                .onSuccess(conn -> {
                    LOG.info("✅ Connected to AMQP broker");

                    conn
                            .createSender(topic)
                            .onSuccess(sender -> vertx.setPeriodic(30_000, timerId -> { //send each 30 s
                                String message = "Hello from Vert.x! " + LocalDateTime.now();
                                sender.send(AmqpMessage.create().withBody(message).build());
                            }))
                            .onFailure(error -> LOG.error("❌ Failed to sender: ", error));

                })
                .onFailure(error -> LOG.error("❌ Connection failed: ", error));
    }
}
