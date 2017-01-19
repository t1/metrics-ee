package com.github.t1.metrics;

import com.codahale.metrics.*;
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
        return Response.status(OK)
                       .entity(new GenericEntity<SortedMap<String, Metric>>(new TreeMap<>(metrics.getMetrics())) {})
                       .type(responseType())
                       .build();
    }

    @GET
    @Path("/-healthchecks")
    public Response getHealthChecks() {
        SortedMap<String, Result> results = healthChecks.runHealthChecks();
        boolean healthy = results.values().stream().allMatch(Result::isHealthy);
        return Response.status(healthy ? OK : INTERNAL_SERVER_ERROR)
                       .entity(new GenericEntity<SortedMap<String, Result>>(new TreeMap<>(results)) {})
                       .type(responseType()).build();
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
