package org.graylog2.plugins.tcplogstashoutput;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.streams.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * the actual plugin implementation which does the work
 * it holds the workers
 */
public class TcpLogstashOutput implements MessageOutput {


    private static final String CK_HOST = "tcp_output_host";
    private static final String CK_PORT = "tcp_output_port";
    private static final String CK_WORKERS = "workers";

    private final Logger LOGGER = LoggerFactory.getLogger(TcpLogstashOutput.class);

    private final BlockingQueue<TcpOutputWorker> workerQueue;
    private final LinkedList<TcpOutputWorker> allWorkers;


    @Inject
    public TcpLogstashOutput(@Assisted Stream stream, @Assisted Configuration configuration) {

        this(configuration.getString(CK_HOST), configuration.getInt(CK_PORT), configuration.getInt(CK_WORKERS));


    }

    /**
     * initialize the plugin
     *
     * @param host    host for tcp connection
     * @param port    port for tcp connection
     * @param workers number of workers
     */

    public TcpLogstashOutput(String host, Integer port, Integer workers) {
        checkPreconditions(host, port, workers);
        allWorkers = new LinkedList<>();
        for (int i = 0; i < workers; i++) {
            TcpOutputWorker worker = new TcpOutputWorker(host, port);
            worker.reconnect();
            allWorkers.add(worker);
        }
        workerQueue = new ArrayBlockingQueue<>(allWorkers.size());
        workerQueue.addAll(allWorkers);


    }

    /**
     * checks iff all parameters are correct
     *
     * @param host    logstash host name
     * @param port    port of the logstash host
     * @param workers number of workers
     */
    private void checkPreconditions(String host, Integer port, Integer workers) {
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("host must not be null");
        }
        if (port == null || port < 1 || port > 65535) {
            throw new IllegalArgumentException("port is invalid");
        }
        if (workers == null || workers < 1) {
            throw new IllegalArgumentException("number of workers must be positive");
        }
    }


    @Override
    public void stop() {
        LOGGER.info("stopping Output ...");
        for (TcpOutputWorker allWorker : allWorkers) {
            allWorker.stop();
        }

    }

    @Override
    public boolean isRunning() {
        return Iterables.any(allWorkers, new Predicate<TcpOutputWorker>() {
            @Override
            public boolean apply(@Nullable TcpOutputWorker tcpOutputWorker) {
                return tcpOutputWorker.isActive();
            }
        });

    }

    @Override
    public void write(Message message) throws Exception {

        write(Arrays.asList(message));
    }

    /**
     * processes the messages: transform into json and send it
     *
     * @param list
     * @throws Exception
     */
    @Override
    public void write(List<Message> list) throws Exception {
        Iterable<String> transform = transformMessages(list);
        processMessages(transform);
    }

    private void processMessages(Iterable<String> transform) throws Exception {
        TcpOutputWorker tcpOutputWorker = workerQueue.take();
        if (!tcpOutputWorker.isActive()) {
            workerQueue.put(tcpOutputWorker);
        } else {
            try {

                tcpOutputWorker.write(transform);
            } finally {

                workerQueue.put(tcpOutputWorker);
            }

        }
    }

    /**
     * transform into json by using the mapping also used for elasticsearch
     *
     * @param list
     * @return
     */
    private Iterable<String> transformMessages(List<Message> list) {
        return Iterables.transform(list, new Function<Message, String>() {
            @Nullable
            @Override
            public String apply(Message message) {
                return toJson(message);
            }
        });
    }

    /**
     * take the same mapping of the mapping like elasticsearch but remove the stream block
     *
     * @param message the message to send
     * @return string representation
     */
    private String toJson(Message message) {
        Map<String, Object> map = message.toElasticSearchObject();
        map.remove("streams");
        map.remove("gl2_remote_port");
        map.remove("gl2_remote_ip");
        map.put("timestamp", message.getTimestamp().getMillis() / 1000.0);

        try {
            return new ObjectMapper().writeValueAsString(map);
        } catch (JsonProcessingException e) {
            LOGGER.warn(String.format("mapping for %s failed", message));
            throw new IllegalArgumentException("Message cannot be transformed into json");
        }

    }


    public interface Factory extends MessageOutput.Factory<TcpLogstashOutput> {
        @Override
        TcpLogstashOutput create(Stream stream, Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }


    public static class Config extends MessageOutput.Config {


        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest configurationRequest = new ConfigurationRequest();

            configurationRequest.addField(new TextField(
                            CK_HOST,
                            "Host",
                            "localhost",
                            "host for tcp endpoint",
                            ConfigurationField.Optional.NOT_OPTIONAL)
            );

            configurationRequest.addField(new NumberField(
                            CK_PORT,
                            "Port",
                            12300,
                            "The port on the host to connect to",
                            ConfigurationField.Optional.NOT_OPTIONAL,
                            NumberField.Attribute.ONLY_POSITIVE)
            );
            configurationRequest.addField(new NumberField(
                            CK_WORKERS,
                            "Number of Workers",
                            1,
                            "Number of workers to be instanciated",
                            ConfigurationField.Optional.NOT_OPTIONAL,
                            NumberField.Attribute.ONLY_POSITIVE)
            );


            return configurationRequest;
        }

    }

    public static class Descriptor extends MessageOutput.Descriptor {

        public Descriptor() {
            super("TCP Logstash Output", false, "", "An output plugin sending messages in JSON format over TCP");
        }
    }

}
