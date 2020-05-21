package com.yo1000.heagent;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

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

        Server server = hostname != null && !hostname.equals("0.0.0.0")
                ? new Server(new InetSocketAddress(hostname, port))
                : new Server(port);

        System.out.println("Building server: " + (hostname != null ? hostname : "127.0.0.1") + ":" + port + path);

        server.setHandler(new ServletHandler() {{
            addServletWithMapping(new ServletHolder(new HealthCheckServlet(loader)), path);
        }});

        return server;
    }
}
