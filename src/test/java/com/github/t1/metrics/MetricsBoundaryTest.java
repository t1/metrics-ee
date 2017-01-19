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
    private Map<String, Metric> metrics(Response response) { return (Map<String, Metric>) response.getEntity(); }

    @Test
    public void shouldGetNoMetricsWith() throws Exception {
        Response response = resource.getMetrics();

        assertThat(metrics(response)).isEmpty();
    }

    @Test
    public void shouldGetOneCounter() throws Exception {
        Counter counter = new Counter();
        metrics.register("foo", counter);

        Response response = resource.getMetrics();

        assertThat(metrics(response)).containsOnly(entry("foo", counter));
    }

    @Test
    public void shouldGetOneGauge() throws Exception {
        Gauge gauge = () -> 3;
        metrics.register("foo", gauge);

        Response response = resource.getMetrics();

        assertThat(metrics(response)).containsOnly(entry("foo", gauge));
    }

    @Test
    public void shouldGetOneHistogram() throws Exception {
        Histogram histogram = new Histogram(new ExponentiallyDecayingReservoir());
        metrics.register("foo", histogram);

        Response response = resource.getMetrics();

        assertThat(metrics(response)).containsOnly(entry("foo", histogram));
    }

    @Test
    public void shouldGetOneMeter() throws Exception {
        Meter meter = new Meter();
        metrics.register("foo", meter);

        Response response = resource.getMetrics();

        assertThat(metrics(response)).containsOnly(entry("foo", meter));
    }

    @Test
    public void shouldGetOneTimer() throws Exception {
        Timer timer = new Timer();
        metrics.register("foo", timer);

        Response response = resource.getMetrics();

        assertThat(metrics(response)).containsOnly(entry("foo", timer));
    }

    @Test
    public void shouldGetOneOtherMetric() throws Exception {
        Metric metric = new Metric() {};
        metrics.register("foo", metric);

        Response response = resource.getMetrics();

        assertThat(metrics(response)).containsOnly(entry("foo", metric));
    }

    @Test
    public void shouldGetDifferentMetrics() throws Exception {
        Counter counter = new Counter();
        metrics.register("foo", counter);
        Gauge gauge = () -> 3;
        metrics.register("bar", gauge);

        Response response = resource.getMetrics();

        assertThat(metrics(response)).containsOnly(entry("foo", counter), entry("bar", gauge));
    }

    @Test
    public void shouldGetMetricSet() throws Exception {
        MetricSet set = new JvmAttributeGaugeSet();
        metrics.register("bar", set);

        Response response = resource.getMetrics();

        assertThat(metrics(response).keySet()).containsOnly("bar.name", "bar.vendor", "bar.uptime");
    }
}
