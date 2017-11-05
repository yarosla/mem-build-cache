package com.github.yarosla.buildcache;

import ch.qos.logback.classic.Level;
import com.beust.jcommander.JCommander;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.*;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;

public class Main {
    private final static Logger logger = LoggerFactory.getLogger(Main.class);

    private static final String PKG_NAME = "build-cache.war";
    private static final String CONTEXT_PATH = "/";
    private static final String SPRING_CONFIG_LOCATION = "com.github.yarosla.buildcache.config";
    private static final String SPRING_SECURITY_FILTER_NAME = "springSecurityFilterChain";
    private static final String SPRING_DISPATCHER_SERVLET_NAME = "DispatcherServlet";
    private static final String SPRING_DISPATCHER_MAPPING_URL = "/*";

    private static Parameters parameters = new Parameters();

    public static void main(String[] args) throws ServletException, InterruptedException {
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

        if (parameters.isSecure()) {
            System.setProperty("spring.profiles.active", "secure");
        }

        Undertow server = configureUndertow();
        server.start();

        logger.info("Build cache is listening on http://{}:{}/cache/", parameters.getHost(), parameters.getPort());

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (server) {
            server.wait(); // block indefinitely
        }
    }

    public static Parameters getParameters() {
        return parameters;
    }

    private static Undertow configureUndertow() throws ServletException {

        WebApplicationContext context = createSpringWebAppContext(SPRING_CONFIG_LOCATION);

        DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(Main.class.getClassLoader())
                .setContextPath(CONTEXT_PATH).setDeploymentName(PKG_NAME)
                .addServlet(createDispatcherServlet(context))
                .addListener(createContextLoaderListener(context));

        if (parameters.isSecure()) {
            servletBuilder
                    .addFilter(new FilterInfo(SPRING_SECURITY_FILTER_NAME, DelegatingFilterProxy.class))
                    .addFilterServletNameMapping(SPRING_SECURITY_FILTER_NAME, SPRING_DISPATCHER_SERVLET_NAME, DispatcherType.REQUEST);
        }

        DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();

        PathHandler path = Handlers.path()
                .addPrefixPath(CONTEXT_PATH, manager.start());

        return Undertow.builder()
                .addHttpListener(parameters.getPort(), parameters.getHost())
                .setHandler(path)
                .build();
    }

    private static WebApplicationContext createSpringWebAppContext(String configLocation) {
        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
        context.setConfigLocation(configLocation);
        return context;
    }

    private static ListenerInfo createContextLoaderListener(WebApplicationContext context) {
        InstanceFactory<ContextLoaderListener> factory = new ImmediateInstanceFactory<>(new ContextLoaderListener(context));
        return new ListenerInfo(ContextLoaderListener.class, factory);
    }

    private static ServletInfo createDispatcherServlet(WebApplicationContext context) {
        InstanceFactory<DispatcherServlet> factory = new ImmediateInstanceFactory<>(new DispatcherServlet(context));
        return Servlets.servlet(SPRING_DISPATCHER_SERVLET_NAME, DispatcherServlet.class, factory)
                .addMapping(SPRING_DISPATCHER_MAPPING_URL)
                .setLoadOnStartup(1);
    }
}
