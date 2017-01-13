package com.github.t1.metrics;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheck.Result;
import com.codahale.metrics.health.HealthCheckRegistry;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.MediaType;
import java.util.*;

import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.*;

@Path("/")
public class MetricsBoundary {
    private static final MediaType DEFAULT_MEDIA_TYPE = APPLICATION_JSON_TYPE;
    @Inject MetricRegistry metrics;
    @Inject HealthCheckRegistry healthChecks;

    @Context HttpHeaders headers;

    @GET
    @Path("/-metrics")
    public Response getMetrics() {
        Map<String, Map<String, Metric>> grouped = new TreeMap<>();
        this.metrics.getMetrics().entrySet().forEach(entry -> grouped
                .computeIfAbsent(name(entry.getValue()), s -> new TreeMap<>())
                .put(entry.getKey(), entry.getValue()));
        return Response.ok(grouped, responseType()).build();
    }

    @SuppressWarnings("ChainOfInstanceofChecks") private String name(Metric metric) {
        if (metric instanceof Counter)
            return "counter";
        else if (metric instanceof Gauge)
            return "gauge";
        else if (metric instanceof Histogram)
            return "histogram";
        else if (metric instanceof Meter)
            return "meter";
        else if (metric instanceof Timer)
            return "timer";
        else
            return "metric";
    }

    @GET
    @Path("/-healthchecks")
    public Response getHealthChecks() {
        SortedMap<String, Result> results = healthChecks.runHealthChecks();
        boolean healthy = results.values().stream().allMatch(Result::isHealthy);
        return Response.status(healthy ? OK : INTERNAL_SERVER_ERROR).entity(results).type(responseType()).build();
    }

    private MediaType responseType() {
        String headerString = headers.getHeaderString(HttpHeaders.ACCEPT);
        if (headerString == null)
            return DEFAULT_MEDIA_TYPE;
        MediaType mediaType = MediaType.valueOf(headerString);
        if (mediaType.isWildcardType())
            return DEFAULT_MEDIA_TYPE;
        return mediaType;
    }
}
