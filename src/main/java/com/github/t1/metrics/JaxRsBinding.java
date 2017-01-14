package com.github.t1.metrics;

import com.codahale.metrics.MetricRegistry;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.container.*;
import javax.ws.rs.ext.Provider;
import java.time.Instant;

import static java.time.temporal.ChronoUnit.*;
import static java.util.concurrent.TimeUnit.*;

@Provider
@Slf4j
public class JaxRsBinding implements ContainerRequestFilter, ContainerResponseFilter {
    private static final String START_INSTANT = JaxRsBinding.class + "#START_INSTANT";

    @Inject MetricRegistry metrics;

    @Override public void filter(ContainerRequestContext requestContext) {
        requestContext.setProperty(START_INSTANT, Instant.now());
    }

    @Override public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Instant stopInstant = Instant.now();
        Instant startInstant = (Instant) requestContext.getProperty(START_INSTANT);
        metrics.timer(
                "resources"
                        + requestContext.getUriInfo().getPath().replace("/", "./")
                        + "."
                        + requestContext.getMethod())
               .update(startInstant.until(stopInstant, MILLIS), MILLISECONDS);
    }
}
