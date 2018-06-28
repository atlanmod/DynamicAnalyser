package com.tblf.linker;

import com.tblf.linker.tracers.MqttTracer;
import com.tblf.linker.tracers.Tracer;
import com.tblf.utils.Configuration;
import io.moquette.server.Server;
import org.fusesource.mqtt.client.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class MqttTracerTest {
    private Server server;
    private Tracer tracer;

    @Before
    public void setUp() throws IOException {
        Configuration.setProperty("mqttHost", "0.0.0.0");
        Configuration.setProperty("mqttPort", "1883");
        File conf = new File("src/test/resources/moquette.conf");
        server = new Server();
        server.startServer(conf);

    }

    @Test
    public void newMqttTracer() throws IOException {
        tracer = new MqttTracer();
    }

    @Test
    public void writeMqttTracer() throws Exception {

        tracer = new MqttTracer();

        MQTT mqtt = new MQTT();
        mqtt.setHost("0.0.0.0", 1883);
        BlockingConnection blockingConnection = mqtt.blockingConnection();
        blockingConnection.connect();
        blockingConnection.subscribe(new Topic[]{new Topic("trace", QoS.AT_MOST_ONCE)});
        tracer.write("testValue");
        Message message =  blockingConnection.receive();
        tracer.endTrace();

        String content = new String(message.getPayload(), Charset.defaultCharset());

        Assert.assertEquals("not the expected value", "testValue", content);
    }

    @Test
    public void writeMqttTracerWithTopic() throws Exception {

        tracer = new MqttTracer();

        MQTT mqtt = new MQTT();
        mqtt.setHost("0.0.0.0", 1883);

        BlockingConnection blockingConnection = mqtt.blockingConnection();
        blockingConnection.connect();
        blockingConnection.subscribe(new Topic[]{new Topic("topic", QoS.AT_MOST_ONCE)});

        tracer.write("topic", "testValue");
        Message message =  blockingConnection.receive();
        tracer.endTrace();

        String content = new String(message.getPayload(), Charset.defaultCharset());

        Assert.assertEquals("not the expected value", "testValue", content);
    }

    @After
    public void tearDown() {
        tracer.endTrace();
        server.stopServer();
    }
}
