package org.graylog2.plugins.tcplogstashoutput;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * worker which does the actual tcp requests and hold the Socker and Stream
 */
public class TcpOutputWorker {
    private final String host;
    private final Integer port;
    Socket socket = null;
    DataOutputStream outToServer = null;
    private final Logger LOGGER = LoggerFactory.getLogger(TcpOutputWorker.class);

    private final AtomicBoolean active = new AtomicBoolean(false);
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);

    public TcpOutputWorker(String host, Integer port) {
        checkPreconditions(host, port);
        this.host = host;
        this.port = port;
    }

    private void checkPreconditions(String host, Integer port) {
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("host must not be empty");
        }
        if (port == null || port < 1 || port > 65535) {
            throw new IllegalArgumentException("port is out of range");
        }
    }

    void stop() {
        active.set(false);
        synchronized (outToServer) {
            IOUtils.closeQuietly(outToServer);
            IOUtils.closeQuietly(socket);
        }
    }

    void reconnect() {
        if (!reconnecting.get()) {
            createReconnectingThread().start();
        }
    }

    private Thread createReconnectingThread() {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                reconnecting.set(true);
                LOGGER.info(String.format("connecting to %s:%s", host, port));


                IOUtils.closeQuietly(socket);

                IOUtils.closeQuietly(outToServer);

                while (!active.get()) {

                    socket = null;
                    try {
                        socket = new Socket(host, port);
                        outToServer = new DataOutputStream(socket.getOutputStream());
                        reconnecting.set(false);
                        active.set(true);
                        LOGGER.info(String.format("connection succeeded to %s:%s", host, port));
                    } catch (Exception e) {
                        active.set(false);
                        IOUtils.closeQuietly(outToServer);
                        IOUtils.closeQuietly(socket);
                        LOGGER.warn(String.format("unable to connect to %s:%s", host, port));
                    } finally {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            //NOOP
                        }
                    }

                }
            }
        });
    }


    public void write(Iterable<String> list) throws Exception {
        if (!active.get()) {

            reconnect();

            throw new IllegalStateException("output is not running");
        }

        try {
            for (String message : list) {
                synchronized (outToServer) {
                    outToServer.writeBytes((message + '\n'));
                    outToServer.flush();
                }
            }


        } catch (IOException ioe) {
            active.set(false);
            reconnect();
            throw new IllegalStateException("output not responding",ioe);
        }
    }

    public boolean isActive() {
        return active.get();
    }
}