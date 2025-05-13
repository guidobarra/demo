package com.guba.vertx.demo.queues;

import io.vertx.core.AbstractVerticle;
import io.vertx.mqtt.MqttClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Consumer extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(Consumer.class);

    @Override
    public void start() {

        MqttClient client = MqttClient.create(vertx);

        client
                .connect(1883, "localhost")
                .onSuccess(ack -> {
                    LOG.info("✅ Connected to MQTT broker, code: {}, isPresent: {}", ack.code(), ack.isSessionPresent());

                    String wildcardTopic = "edificio1/+/temperatura";

                    client
                            .subscribe(wildcardTopic, 2, sub -> LOG.info("📡 Subscrito a: {}", wildcardTopic))
                            .publishHandler(s -> {
                                LOG.info("There are new message in topic: {}", s.topicName());
                                LOG.info("Content(as string) of the message: {}", s.payload());
                                LOG.info("QoS: {}", s.qosLevel());
                            });

                    /*
                    client
                            .publishHandler(s -> {
                                LOG.info("There are new message in topic: {}", s.topicName());
                                LOG.info("Content(as string) of the message: {}", s.payload());
                                LOG.info("QoS: {}", s.qosLevel());
                            })
                            .subscribe(wildcardTopic, 2);*/

                })
                .onFailure(error -> {
                    client.disconnect();
                    LOG.error("❌ Connection failed: ", error);
                });


    }
}
