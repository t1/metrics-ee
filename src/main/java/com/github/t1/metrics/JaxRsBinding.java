package com.github.t1.metrics;

import com.codahale.metrics.MetricRegistry;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.client.*;
import javax.ws.rs.container.*;
import javax.ws.rs.core.Response.StatusType;
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
                response.getStatusInfo(),
                request.getProperty(START_INSTANT));
    }

    private String name(String type, String path, String method) {
        if (!path.startsWith("/"))
            path = "/" + path; // Dropwizard quirk
        return type + path + "|" + method;
    }

    @Override
    public void filter(ClientRequestContext request) { request.setProperty(START_INSTANT, Instant.now()); }

    @Override
    public void filter(ClientRequestContext request, ClientResponseContext response) {
        updateTimer(name("calls", request.getUri().getPath(), request.getMethod()),
                response.getStatusInfo(),
                request.getProperty(START_INSTANT));
    }

    private void updateTimer(String name, StatusType status, Object startInstant) {
        long time = (startInstant == null) ? -1 : ((Temporal) startInstant).until(Instant.now(), MILLIS);
        metrics.timer(name + "|timer").update(time, MILLISECONDS);
        metrics.meter(name + "|" + status.getFamily()).mark();
        metrics.meter(name + "|" + status.getStatusCode()).mark();
    }
}
