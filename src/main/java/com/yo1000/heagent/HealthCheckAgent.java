package com.yo1000.heagent;

import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;

import java.io.UnsupportedEncodingException;
import java.lang.instrument.Instrumentation;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.stream.Collectors;

public class HealthCheckAgent {
    private static final String PARAMETER_PREFIX = "heagent-";
    private static final int DEFAULT_PORT = 8889;

    public static void premain(final String agentArgs, final Instrumentation instrumentation) {
        final Thread t = new Thread(() -> {
            final Server server = createServer(agentArgs);

            try {
                server.start();
                server.join();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        });

        t.setDaemon(true);
        t.start();
    }

    public static Server createServer(final String agentArgs) {
        final ParameterLoader loader = new ParameterLoader(agentArgs, PARAMETER_PREFIX);

        final String hostname = loader.loadByName("hostname", null, (s, defaultValue) -> s);
        final int port = loader.loadByName("port", DEFAULT_PORT, (s, defaultValue) -> Integer.parseInt(s));
        final String path = loader.loadByName("path", "/health", (s, defaultValue) ->
                "/" + Arrays.stream(s.replaceAll("^/", "").split("/"))
                        .map(part -> {
                            try {
                                return URLEncoder.encode(part, "utf-8");
                            } catch (final UnsupportedEncodingException e) {
                                throw new IllegalStateException(e);
                            }
                        })
                        .collect(Collectors.joining("/")));


        final Server server = new Server(new QueuedThreadPool() {{ setDaemon(true); }});

        server.addConnector(new ServerConnector(
                server, null, new ScheduledExecutorScheduler("Connector-Scheduler-Daemonized", true),
                null, -1, -1, new HttpConnectionFactory()) {{
            setHost(hostname != null ? hostname : "0.0.0.0");
            setPort(port);
        }});

        System.out.println("Building server: " + (hostname != null ? hostname : "0.0.0.0") + ":" + port + path);

        server.setHandler(new ServletHandler() {{
            addServletWithMapping(new ServletHolder(new HealthCheckServlet(loader)), path);
        }});

        return server;
    }
}
