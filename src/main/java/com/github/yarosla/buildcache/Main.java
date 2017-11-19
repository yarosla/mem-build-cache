package com.github.yarosla.buildcache;

import ch.qos.logback.classic.Level;
import com.beust.jcommander.JCommander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.ipc.netty.http.server.HttpServer;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.PUT;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.RouterFunctions.toHttpHandler;

public class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {
        Parameters parameters = new Parameters();
        JCommander jCommander = JCommander.newBuilder()
                .addObject(parameters)
                .build();
        jCommander.parse(args);

        if (parameters.isHelp()) {
            jCommander.usage();
            return;
        }

        if (parameters.isDebug()) {
            ch.qos.logback.classic.Logger packageLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("com.github.yarosla.buildcache");
            packageLogger.setLevel(Level.DEBUG);
        }

        HttpServer.create(parameters.getHost(), parameters.getPort())
                .newHandler(new ReactorHttpHandlerAdapter(toHttpHandler(buildRouter(parameters))))
                .block();

        logger.info("Build cache is listening on http://{}:{}/cache/", parameters.getHost(), parameters.getPort());

        Thread.currentThread().join(); // block indefinitely
    }

    private static RouterFunction<ServerResponse> buildRouter(Parameters parameters) {
        BuildCache buildCache = new BuildCache(parameters.getMemoryLimit());

        String cachePattern = "/cache/{*" + BuildCache.PATH_VAR_NAME + "}";
        RouterFunction<ServerResponse> router = route(GET(cachePattern), buildCache::get)
                .andRoute(PUT(cachePattern), buildCache::put)
                .andRoute(GET("/stats"), buildCache::statistics);

        if (parameters.isSecure()) {
            HttpBasicAuth httpBasicAuth = new HttpBasicAuth(parameters.getUsername(), parameters.getPassword());
            router = router.filter(httpBasicAuth::filter);
        }
        return router;
    }
}
