package com.github.t1.metrics;

import com.codahale.metrics.health.*;
import com.codahale.metrics.health.HealthCheck.Result;
import org.assertj.core.api.MapAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.core.*;
import java.util.Map;

import static javax.ws.rs.core.HttpHeaders.*;
import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.Status.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HealthChecksBoundaryTest {
    @InjectMocks MetricsBoundary resource;
    @Spy HealthCheckRegistry healthChecks;
    @Mock HttpHeaders headers;

    private static HealthCheck check(Result result) {
        return new HealthCheck() {
            @Override protected Result check() throws Exception { return result; }
        };
    }

    @SuppressWarnings("unchecked")
    private MapAssert<String, Result> assertThatHealthChecks(Response response) {
        return assertThat((Map<String, Result>) response.getEntity());
    }

    @Test
    public void shouldGetNoHealthCheck() throws Exception {
        Response response = resource.getHealthChecks();

        assertThat(response.getHeaderString(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON);
        assertThatHealthChecks(response).isEmpty();
    }

    @Test
    public void shouldGetOneHealthyCheckWithoutAcceptHeader() throws Exception {
        healthChecks.register("foo", check(Result.healthy()));

        Response response = resource.getHealthChecks();

        assertThat(response.getHeaderString(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON);
        assertThat(response.getStatusInfo()).isEqualTo(OK);
        assertThatHealthChecks(response).containsOnly(entry("foo", Result.healthy()));
    }

    @Test
    public void shouldGetOneHealthyCheckWithWildcardAcceptHeader() throws Exception {
        when(headers.getHeaderString(ACCEPT)).thenReturn(WILDCARD);
        healthChecks.register("foo", check(Result.healthy()));

        Response response = resource.getHealthChecks();

        assertThat(response.getHeaderString(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON);
        assertThatHealthChecks(response).containsOnly(entry("foo", Result.healthy()));
    }

    @Test
    public void shouldGetOneHealthyCheckWithXmlAcceptHeader() throws Exception {
        healthChecks.register("foo", check(Result.healthy()));
        when(headers.getHeaderString(ACCEPT)).thenReturn(APPLICATION_XML);

        Response response = resource.getHealthChecks();

        assertThat(response.getHeaderString(CONTENT_TYPE)).isEqualTo(APPLICATION_XML);
        assertThatHealthChecks(response).containsOnly(entry("foo", Result.healthy()));
    }

    @Test
    public void shouldGetOneUnhealthyCheck() throws Exception {
        healthChecks.register("foo", check(Result.unhealthy("bar")));

        Response response = resource.getHealthChecks();

        assertThat(response.getStatusInfo()).isEqualTo(INTERNAL_SERVER_ERROR);
        assertThatHealthChecks(response).containsOnly(entry("foo", Result.unhealthy("bar")));
    }

    @Test
    public void shouldGetTwoHealthyChecks() throws Exception {
        healthChecks.register("foo", check(Result.healthy()));
        healthChecks.register("bar", check(Result.healthy()));

        Response response = resource.getHealthChecks();

        assertThatHealthChecks(response).containsOnly(entry("foo", Result.healthy()), entry("bar", Result.healthy()));
    }

    @Test
    public void shouldGetTwoUnhealthyChecks() throws Exception {
        healthChecks.register("foo", check(Result.unhealthy("foo-x")));
        healthChecks.register("bar", check(Result.unhealthy("bar-x")));

        Response response = resource.getHealthChecks();

        assertThatHealthChecks(response).containsOnly(
                entry("foo", Result.unhealthy("foo-x")),
                entry("bar", Result.unhealthy("bar-x")));
    }
}
