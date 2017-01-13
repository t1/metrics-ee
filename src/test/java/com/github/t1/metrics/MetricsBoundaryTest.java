package com.github.t1.metrics;

import com.codahale.metrics.*;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheck.Result;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.core.*;
import java.util.Map;

import static javax.ws.rs.core.HttpHeaders.*;
import static javax.ws.rs.core.MediaType.*;
import static org.assertj.core.api.Assertions.*;

@RunWith(MockitoJUnitRunner.class)
public class MetricsBoundaryTest {
    @InjectMocks MetricsBoundary resource;
    @Spy MetricRegistry metrics;
    @Mock HttpHeaders headers;

    private static HealthCheck check(Result result) {
        return new HealthCheck() {
            @Override protected Result check() throws Exception { return result; }
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Metric>> metrics(Response response) {
        return (Map<String, Map<String, Metric>>) response.getEntity();
    }

    @Test
    public void shouldGetNoMetrics() throws Exception {
        Response response = resource.getMetrics();

        assertThat(response.getHeaderString(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON);
        assertThat(metrics(response)).isEmpty();
    }

    @Test
    public void shouldGetOneCounter() throws Exception {
        Counter counter = new Counter();
        metrics.register("foo", counter);

        Response response = resource.getMetrics();

        assertThat(response.getHeaderString(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON);
        assertThat(metrics(response).get("counter")).containsOnly(entry("foo", counter));
    }

    @Test
    public void shouldGetOneGauge() throws Exception {
        Gauge gauge = () -> 3;
        metrics.register("foo", gauge);

        Response response = resource.getMetrics();

        assertThat(response.getHeaderString(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON);
        assertThat(metrics(response).get("gauge")).containsOnly(entry("foo", gauge));
    }

    @Test
    public void shouldGetOneOtherMetric() throws Exception {
        Metric metric = new Metric() {};
        metrics.register("foo", metric);

        Response response = resource.getMetrics();

        assertThat(response.getHeaderString(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON);
        assertThat(metrics(response).get("other")).containsOnly(entry("foo", metric));
    }
}
