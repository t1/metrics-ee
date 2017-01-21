package com.github.t1.metrics;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import com.github.t1.testtools.OrderedJUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.*;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.github.t1.metrics.MetricsYamlMessageBodyWriter.*;
import static java.time.temporal.ChronoUnit.*;
import static javax.ws.rs.core.MediaType.*;
import static org.assertj.core.api.Assertions.*;

@RunWith(OrderedJUnitRunner.class)
public class MetricsYamlMessageBodyWriterTest {
    private final MetricRegistry metrics = new MetricRegistry();
    private final MetricsYamlMessageBodyWriter writer = new MetricsYamlMessageBodyWriter();

    private Counter counter(long count) {
        Counter counter = new Counter();
        counter.inc(count);
        return counter;
    }

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
            writer.writeTo(metrics(), type, type, new Annotation[0], APPLICATION_YAML_TYPE, null, out);
            return out.toString("UTF-8");
        }
    }

    private SortedMap<String, Metric> metrics() { return new TreeMap<>(metrics.getMetrics()); }

    @Test
    public void shouldBeWritable() throws Exception {
        boolean writeable = writer.isWriteable(MetricRegistry.class, METRICS_MAP.getType(), null,
                APPLICATION_YAML_TYPE);

        assertThat(writeable).isTrue();
    }

    @Test
    public void shouldNotWriteWrongType() throws Exception {
        boolean writeable = writer.isWriteable(Metric.class, Metric.class, null, APPLICATION_YAML_TYPE);

        assertThat(writeable).isFalse();
    }

    @Test
    public void shouldNotWriteWrongMediaType() throws Exception {
        boolean writeable = writer.isWriteable(MetricRegistry.class, MetricRegistry.class, null, APPLICATION_XML_TYPE);

        assertThat(writeable).isFalse();
    }

    @Test
    public void shouldNotWriteWrongTypeAndMediaType() throws Exception {
        boolean writeable = writer.isWriteable(Metric.class, Metric.class, null, APPLICATION_XML_TYPE);

        assertThat(writeable).isFalse();
    }

    @Test
    public void shouldHaveSizeMinusOne() throws Exception {
        long size = writer.getSize(metrics(), MetricRegistry.class, MetricRegistry.class, null,
                APPLICATION_YAML_TYPE);

        assertThat(size).isEqualTo(-1);
    }

    @Test
    public void shouldWriteEmptyMetrics() throws Exception {
        String out = write();

        assertThat(out).isEqualTo("\n");
    }

    @Test
    public void shouldWriteOneCounter() throws Exception {
        metrics.register("foo", counter(3));

        String out = write();

        assertThat(out).isEqualTo("\n"
                + "foo: 3\n");
    }

    @Test
    public void shouldWriteOneGauge() throws Exception {
        metrics.register("foo", (Gauge) () -> 1);

        String out = write();

        assertThat(out).isEqualTo("\n"
                + "foo: 1\n");
    }

    @Test
    public void shouldWriteTwoGauges() throws Exception {
        metrics.register("foo", (Gauge) () -> 1);
        metrics.register("bar", (Gauge) () -> 2);

        String out = write();

        assertThat(out).isEqualTo("\n"
                + "bar: 2\n"
                + "foo: 1\n");
    }

    @Test
    public void shouldWriteOneHistogram() throws Exception {
        metrics.register("foo", histogram());

        String out = write();

        assertThat(out).isEqualTo("\n"
                + "foo:\n"
                + "  count: 4\n"
                + "  min: 2\n"
                + "  mean: 8.99448616284921\n"
                + "  max: 9\n"
                + "  stddev: 0.19461071021198567\n"
                + "  p50: 9.0\n"
                + "  p75: 9.0\n"
                + "  p95: 9.0\n"
                + "  p98: 9.0\n"
                + "  p99: 9.0\n"
                + "  p999: 9.0\n");
    }

    @Test
    public void shouldWriteOneMeter() throws Exception {
        metrics.register("foo", meter());

        String out = write();

        assertThat(out).isEqualTo("\n"
                + "foo:\n"
                + "  count: 18\n"
                + "  mean_rate: 0.023076923076923078\n"
                + "  m1_rate: 8.44290068149477E-6\n"
                + "  m5_rate: 0.048504583236971655\n"
                + "  m15_rate: 0.2579529801079934\n");
    }

    @Test
    public void shouldWriteOneTimer() throws Exception {
        metrics.register("foo", timer());

        String out = write();

        assertThat(out).isEqualTo("\n"
                + "foo:\n"
                + "  count: 4\n"
                + "  mean_rate: 0.005797101449275362\n"
                + "  m1_rate: 8.32298920294135E-6\n"
                + "  m5_rate: 0.021439832209417236\n"
                + "  m15_rate: 0.09461821810107368\n"
                + "  min: 12000000\n"
                + "  mean: 1.8E7\n"
                + "  max: 25000000\n"
                + "  stddev: 4949747.468305833\n"
                + "  p50: 2.0E7\n"
                + "  p75: 2.5E7\n"
                + "  p95: 2.5E7\n"
                + "  p98: 2.5E7\n"
                + "  p99: 2.5E7\n"
                + "  p999: 2.5E7\n");
    }

    @Test
    public void shouldWriteOneCustomMetric() throws Exception {
        metrics.register("foo", new Metric() {
            @Override public String toString() { return "bar"; }
        });

        String out = write();

        assertThat(out).isEqualTo("\n"
                + "foo: bar\n");
    }

    @Test
    public void shouldWriteOneWithDot() throws Exception {
        metrics.register("foo.bar", (Gauge) () -> 1);

        String out = write();

        assertThat(out).isEqualTo("\n"
                + "foo:\n"
                + "  bar: 1\n");
    }

    @Test
    public void shouldWriteTwoWithDot() throws Exception {
        metrics.register("foo.bar", (Gauge) () -> 1);
        metrics.register("foo.baz", (Gauge) () -> 2);

        String out = write();

        assertThat(out).isEqualTo("\n"
                + "foo:\n"
                + "  bar: 1\n"
                + "  baz: 2\n");
    }

    @Test
    public void shouldWriteManyWithDots() throws Exception {
        metrics.register("aaa.bbb.ccc.ddd", (Gauge) () -> 1);
        metrics.register("aaa.bbb.ccc.eee", counter(2));
        metrics.register("aaa.bbb.ccc.fff", (Gauge) () -> 3);
        metrics.register("aaa.bbb.ggg", (Gauge) () -> 4);
        metrics.register("aaa.bbb.hhh", (Gauge) () -> 5);
        metrics.register("jjj", (Gauge) () -> 6);
        metrics.register("aaa.iii", counter(0)); // will be sorted in

        String out = write();

        assertThat(out).isEqualTo("\n"
                + "aaa:\n"
                + "  bbb:\n"
                + "    ccc:\n"
                + "      ddd: 1\n"
                + "      eee: 2\n"
                + "      fff: 3\n"
                + "    ggg: 4\n"
                + "    hhh: 5\n"
                + "  iii: 0\n"
                + "jjj: 6\n");
    }

    @Test
    public void shouldWriteOneWithSlash() throws Exception {
        metrics.register("foo/bar", (Gauge) () -> 1);

        String out = write();

        assertThat(out).isEqualTo("\n"
                + "foo:\n"
                + "  /bar: 1\n");
    }

    @Test
    public void shouldWriteOneWithDotAndSlash() throws Exception {
        metrics.register("foo.bar/baz", (Gauge) () -> 1);

        String out = write();

        assertThat(out).isEqualTo("\n"
                + "foo:\n"
                + "  bar:\n"
                + "    /baz: 1\n");
    }

    @Test
    public void shouldWriteOneWithSlashAndDot() throws Exception {
        metrics.register("foo/bar.baz", (Gauge) () -> 1);

        String out = write();

        assertThat(out).isEqualTo("\n"
                + "foo:\n"
                + "  /bar.baz: 1\n");
    }

    @Test
    public void shouldWriteOneWithSlashAndPipe() throws Exception {
        metrics.register("foo/bar|baz", (Gauge) () -> 1);

        String out = write();

        assertThat(out).isEqualTo("\n"
                + "foo:\n"
                + "  /bar:\n"
                + "    baz: 1\n");
    }

    @Test
    public void shouldWriteOneWithPipeAndSlash() throws Exception {
        metrics.register("foo|bar/baz", (Gauge) () -> 1);

        String out = write();

        assertThat(out).isEqualTo("\n"
                + "foo|bar:\n"
                + "  /baz: 1\n");
    }

    @Test
    public void shouldWriteOneWithDotAndPipe() throws Exception {
        metrics.register("foo.bar|baz", (Gauge) () -> 1);

        String out = write();

        assertThat(out).isEqualTo("\n"
                + "foo:\n"
                + "  bar|baz: 1\n");
    }

    @Test
    public void shouldWriteOneWithPipeAndDot() throws Exception {
        metrics.register("foo|bar.baz", (Gauge) () -> 1);

        String out = write();

        assertThat(out).isEqualTo("\n"
                + "foo|bar:\n"
                + "  baz: 1\n");
    }

    @Test
    public void shouldWriteOneWithDotAndSlashAndPipe() throws Exception {
        metrics.register("foo.bar/baz|bee", (Gauge) () -> 1);

        String out = write();

        assertThat(out).isEqualTo("\n"
                + "foo:\n"
                + "  bar:\n"
                + "    /baz:\n"
                + "      bee: 1\n");
    }

    @Test
    public void shouldWriteOneWithSlashAndDotAndPipe() throws Exception {
        metrics.register("foo/bar.baz|bee", (Gauge) () -> 1);

        String out = write();

        assertThat(out).isEqualTo("\n"
                + "foo:\n"
                + "  /bar.baz:\n"
                + "    bee: 1\n");
    }

    @Test
    public void shouldWriteOneWithPipeAndSlashAndDot() throws Exception {
        metrics.register("foo|bar/baz.bee", (Gauge) () -> 1);

        String out = write();

        assertThat(out).isEqualTo("\n"
                + "foo|bar:\n"
                + "  /baz.bee: 1\n");
    }

    @Test
    public void shouldWriteOneWithDotAndPipeAndSlash() throws Exception {
        metrics.register("foo.bar|baz/bee", (Gauge) () -> 1);

        String out = write();

        assertThat(out).isEqualTo("\n"
                + "foo:\n"
                + "  bar|baz:\n"
                + "    /bee: 1\n");
    }
}
