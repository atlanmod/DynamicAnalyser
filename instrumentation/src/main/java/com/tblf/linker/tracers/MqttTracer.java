package com.tblf.linker.tracers;

import com.tblf.utils.Configuration;
import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;

import java.io.File;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MqttTracer implements Tracer{
    private static final Logger LOGGER = Logger.getLogger("MqttTracer");
    private BlockingConnection blockingConnection;

    public MqttTracer() {
        MQTT mqtt = new MQTT();
        try {
            mqtt.setHost(Configuration.getProperty("mqttHost"), Integer.parseInt(Configuration.getProperty("mqttPort")));
            blockingConnection = mqtt.blockingConnection();
            blockingConnection.connect();
        } catch (URISyntaxException e) {
            LOGGER.log(Level.WARNING, "Could not initialize MQTT connection", e);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not open a blocking connection", e);
        }
    }

    @Override
    public void write(String value) {
        try {
            blockingConnection.publish("trace", value.getBytes(), QoS.AT_MOST_ONCE, false);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not publish", e);
        }
    }

    @Override
    public void write(String topic, String value) {
        try {
            blockingConnection.publish(topic, value.getBytes(), QoS.AT_MOST_ONCE, false);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not publish", e);
        }
    }

    @Override
    public void updateTest(String className, String methodName) {

    }

    @Override
    public void updateTarget(String className, String methodName) {

    }

    @Override
    public void updateStatementsUsingColumn(String startPos, String endPos) {

    }

    @Override
    public void updateStatementsUsingLine(String line) {

    }

    @Override
    public void startTrace() {

    }


    @Override
    public void endTrace() {
        try {
            blockingConnection.disconnect();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not close the connection", e);
        }
    }

    @Override
    public File getFile() {
        return null;
    }

    @Override
    public void close() throws Exception {

    }
}
