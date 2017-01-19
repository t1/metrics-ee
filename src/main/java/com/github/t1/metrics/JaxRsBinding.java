package com.github.t1.metrics;

import com.codahale.metrics.MetricRegistry;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.client.*;
import javax.ws.rs.container.*;
import javax.ws.rs.ext.Provider;
import java.time.Instant;
import java.time.temporal.Temporal;

import static java.time.temporal.ChronoUnit.*;
import static java.util.concurrent.TimeUnit.*;

@Provider
@Slf4j
public class JaxRsBinding implements
        ContainerRequestFilter, ContainerResponseFilter,
        ClientRequestFilter, ClientResponseFilter {
    private static final String START_INSTANT = JaxRsBinding.class + "#START_INSTANT";

    @Inject MetricRegistry metrics;

    @Override
    public void filter(ContainerRequestContext request) { request.setProperty(START_INSTANT, Instant.now()); }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        updateTimer(name("resources", request.getUriInfo().getPath(), request.getMethod()),
                request.getProperty(START_INSTANT));
    }

    private String name(String type, String path, String method) {
        if (!path.startsWith("/"))
            path = "/" + path; // Dropwizard
        return type + path.replace("/", "./") + "." + method;
    }

    @Override
    public void filter(ClientRequestContext request) { request.setProperty(START_INSTANT, Instant.now()); }

    @Override
    public void filter(ClientRequestContext request, ClientResponseContext response) {
        updateTimer(name("calls", request.getUri().getPath(), request.getMethod()),
                request.getProperty(START_INSTANT));
    }

    private void updateTimer(String name, Object startInstant) {
        metrics.timer(name).update(((Temporal) startInstant).until(Instant.now(), MILLIS), MILLISECONDS);
    }
}
