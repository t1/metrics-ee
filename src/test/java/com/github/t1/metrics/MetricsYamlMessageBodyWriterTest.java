package com.github.t1.metrics;

import com.codahale.metrics.*;
import org.junit.Test;

import java.io.*;
import java.lang.annotation.Annotation;
import java.util.concurrent.TimeUnit;

import static com.github.t1.metrics.MetricsYamlMessageBodyWriter.*;
import static java.time.temporal.ChronoUnit.*;
import static org.assertj.core.api.Assertions.*;

public class MetricsYamlMessageBodyWriterTest {
    private final MetricRegistry metrics = new MetricRegistry();
    private final MetricsYamlMessageBodyWriter writer = new MetricsYamlMessageBodyWriter();

    private Meter meter() {
        MockClock clock = new MockClock();
        Meter meter = new Meter(clock);
        meter.mark(3);
        clock.plus(1, MINUTES);
        meter.mark(5);
        clock.plus(3, MINUTES);
        meter.mark(3);
        clock.plus(9, MINUTES);
        meter.mark(7);
        return meter;
    }

    private Histogram histogram() {
        MockClock clock = new MockClock();
        Histogram histogram = new Histogram(new ExponentiallyDecayingReservoir(1028, 0.015, clock));
        histogram.update(3);
        clock.plus(30, SECONDS);
        histogram.update(7);
        clock.plus(3, MINUTES);
        histogram.update(2);
        clock.plus(8, MINUTES);
        histogram.update(9);
        return histogram;
    }

    private Timer timer() {
        MockClock clock = new MockClock();
        Timer timer = new Timer(new ExponentiallyDecayingReservoir(), clock);
        timer.update(15, TimeUnit.MILLISECONDS);
        clock.plus(30, SECONDS);
        timer.update(20, TimeUnit.MILLISECONDS);
        clock.plus(3, MINUTES);
        timer.update(25, TimeUnit.MILLISECONDS);
        clock.plus(8, MINUTES);
        timer.update(12, TimeUnit.MILLISECONDS);
        return timer;
    }


    private String write() throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Class<MetricRegistry> type = MetricRegistry.class;
            writer.writeTo(metrics, type, type, new Annotation[0], APPLICATION_YAML_TYPE, null, out);
            return out.toString("UTF-8");
        }
    }


    @Test
    public void shouldWriteEmptyMetrics() throws Exception {
        String out = write();

        assertThat(out).isEqualTo(""
                + "counters:\n"
                + "gauges:\n"
                + "meters:\n"
                + "histograms:\n"
                + "timers:\n");
    }

    @Test
    public void shouldWriteOneCounter() throws Exception {
        metrics.register("foo", new Counter());

        String out = write();

        assertThat(out).contains(""
                + "counters:\n"
                + "  foo: 0\n");
    }

    @Test
    public void shouldWriteOneGauge() throws Exception {
        metrics.register("foo", (Gauge) () -> 1);

        String out = write();

        assertThat(out).contains(""
                + "gauges:\n"
                + "  foo: 1\n");
    }

    @Test
    public void shouldWriteTwoGauges() throws Exception {
        metrics.register("foo", (Gauge) () -> 1);
        metrics.register("bar", (Gauge) () -> 2);

        String out = write();

        assertThat(out).contains(""
                + "gauges:\n"
                + "  bar: 2\n"
                + "  foo: 1\n");
    }

    @Test
    public void shouldWriteOneGaugeWithDot() throws Exception {
        metrics.register("foo.bar", (Gauge) () -> 1);

        String out = write();

        assertThat(out).contains(""
                + "gauges:\n"
                + "  foo:\n"
                + "    bar: 1\n");
    }

    @Test
    public void shouldWriteTwoGaugesWithDot() throws Exception {
        metrics.register("foo.bar", (Gauge) () -> 1);
        metrics.register("foo.baz", (Gauge) () -> 2);

        String out = write();

        assertThat(out).contains(""
                + "gauges:\n"
                + "  foo:\n"
                + "    bar: 1\n"
                + "    baz: 2\n");
    }

    @Test
    public void shouldWriteManyGaugesWithDots() throws Exception {
        metrics.register("aaa.bbb", new Counter());
        metrics.register("aaa.bbb.ccc.ddd", (Gauge) () -> 1);
        metrics.register("aaa.bbb.ccc.eee", (Gauge) () -> 2);
        metrics.register("aaa.bbb.ccc.fff", (Gauge) () -> 3);
        metrics.register("aaa.bbb.ggg", (Gauge) () -> 4);
        metrics.register("aaa.bbb.hhh", (Gauge) () -> 5);
        metrics.register("iii", (Gauge) () -> 6);

        String out = write();

        assertThat(out).isEqualTo(""
                + "counters:\n"
                + "  aaa:\n"
                + "    bbb: 0\n"
                + "gauges:\n"
                + "  aaa:\n"
                + "    bbb:\n"
                + "      ccc:\n"
                + "        ddd: 1\n"
                + "        eee: 2\n"
                + "        fff: 3\n"
                + "      ggg: 4\n"
                + "      hhh: 5\n"
                + "  iii: 6\n"
                + "meters:\n"
                + "histograms:\n"
                + "timers:\n");
    }

    @Test
    public void shouldWriteOneHistogram() throws Exception {
        metrics.register("foo", histogram());

        String out = write();

        assertThat(out).contains(""
                + "histograms:\n"
                + "  foo:\n"
                + "    count: 4\n"
                + "    max: 9\n"
                + "    mean: 8.99448616284921\n"
                + "    min: 2\n"
                + "    p50: 9.0\n"
                + "    p75: 9.0\n"
                + "    p95: 9.0\n"
                + "    p98: 9.0\n"
                + "    p99: 9.0\n"
                + "    p999: 9.0\n");
    }

    @Test
    public void shouldWriteOneMeter() throws Exception {
        metrics.register("foo", meter());

        String out = write();

        assertThat(out).contains(""
                + "meters:\n"
                + "  foo:\n"
                + "    count: 18\n"
                + "    m15_rate: 0.2579529801079934\n"
                + "    m1_rate: 8.44290068149477E-6\n"
                + "    m5_rate: 0.048504583236971655\n"
                + "    mean_rate: 0.023076923076923078\n");
    }

    @Test
    public void shouldWriteOneTimer() throws Exception {
        metrics.register("foo", timer());

        String out = write();

        assertThat(out).contains(""
                + "timers:\n"
                + "  foo:\n"
                + "    count: 4\n"
                + "    m15_rate: 0.09461821810107368\n"
                + "    m1_rate: 8.32298920294135E-6\n"
                + "    m5_rate: 0.021439832209417236\n"
                + "    mean_rate: 0.005797101449275362\n"
                + "    max: 25000000\n"
                + "    mean: 1.8E7\n"
                + "    min: 12000000\n"
                + "    p50: 2.0E7\n"
                + "    p75: 2.5E7\n"
                + "    p95: 2.5E7\n"
                + "    p98: 2.5E7\n"
                + "    p99: 2.5E7\n"
                + "    p999: 2.5E7\n");
    }
}
