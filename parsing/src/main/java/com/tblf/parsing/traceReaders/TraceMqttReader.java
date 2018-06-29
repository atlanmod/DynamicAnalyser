package com.tblf.parsing.traceReaders;

import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mqtt client getting the traces from a broker
 */
public class TraceMqttReader extends TraceReader {

    private static final Logger LOGGER = Logger.getLogger(TraceMqttReader.class.getName());
    private BlockingConnection blockingConnection;

    public TraceMqttReader() {
    }

    /**
     * Constructor. Takes a property files as a parameter, e.g.:
     * # host: 0.0.0.0
     * # port: 1883
     * # topic: trace
     * @param conf
     */
    public TraceMqttReader(File conf) {
        setFile(conf);
    }

    @Override
    public void setFile(File file) {
        super.setFile(file);
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
        }

        MQTT mqtt = new MQTT();
        try {
            mqtt.setHost(properties.getProperty("host"), Integer.parseInt(properties.getProperty("port")));
        } catch (URISyntaxException e) {
            LOGGER.log(Level.WARNING, "Could not log to MQTT Broker", e);
        }

        blockingConnection = mqtt.blockingConnection();
        try {
            blockingConnection.subscribe(new Topic[]{new Topic(properties.getProperty("topic"), QoS.AT_MOST_ONCE)});
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could subscribe to the topic "+properties.getProperty("topic"), e);
        }
    }

    @Override
    public String read() {
        try {
            return new String(blockingConnection.receive().getPayload(), Charset.defaultCharset());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could get message.", e);
        }
        return null;
    }
}
