package com.yo1000.heagent;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;

import java.io.UnsupportedEncodingException;
import java.lang.instrument.Instrumentation;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.stream.Collectors;

public class HealthCheckAgent {
    private static final String PARAMETER_PREFIX = "heagent-";
    private static final int DEFAULT_PORT = 8889;

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        Thread t = new Thread(() -> {
            Server server = createServer(agentArgs);

            try {
                server.start();
                server.join();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        t.setDaemon(true);
        t.start();
    }

    public static Server createServer(String agentArgs) {
        ParameterLoader loader = new ParameterLoader(agentArgs, PARAMETER_PREFIX);

        String hostname = loader.loadByName("hostname", null, (s, defaultValue) -> s);
        int port = loader.loadByName("port", DEFAULT_PORT, (s, defaultValue) -> Integer.parseInt(s));
        String path = loader.loadByName("path", "/health", (s, defaultValue) ->
                "/" + Arrays.stream(s.replaceAll("^/", "").split("/"))
                        .map(part -> {
                            try {
                                return URLEncoder.encode(part, "utf-8");
                            } catch (UnsupportedEncodingException e) {
                                throw new IllegalStateException(e);
                            }
                        })
                        .collect(Collectors.joining("/")));


        QueuedThreadPool queuedThreadPool = new QueuedThreadPool();
        queuedThreadPool.setDaemon(true);
        Server server = new Server(queuedThreadPool);

        ScheduledExecutorScheduler scheduledExecutorScheduler = new ScheduledExecutorScheduler("Connector-Scheduler-Daemonized", true);
        ServerConnector connector = new ServerConnector(server, null, scheduledExecutorScheduler, null, -1, -1, new HttpConnectionFactory());
        connector.setHost(hostname != null ? hostname : "127.0.0.1");
        connector.setPort(port);
        server.setConnectors(new Connector[]{connector});

        System.out.println("Building server: " + (hostname != null ? hostname : "127.0.0.1") + ":" + port + path);

        server.setHandler(new ServletHandler() {{
            addServletWithMapping(new ServletHolder(new HealthCheckServlet(loader)), path);
        }});

        return server;
    }
}
