package com.github.t1.metrics;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.*;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;

import static com.github.t1.metrics.MetricsYamlMessageBodyWriter.*;

@Provider
@Produces(APPLICATION_YAML)
@Slf4j
public class MetricsYamlMessageBodyWriter implements MessageBodyWriter<MetricRegistry> {
    public static final String APPLICATION_YAML = "application/yaml";
    public static final MediaType APPLICATION_YAML_TYPE = MediaType.valueOf(APPLICATION_YAML);


    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return MetricRegistry.class.equals(type) && mediaType.isCompatible(APPLICATION_YAML_TYPE);
    }

    @Override
    public long getSize(MetricRegistry r, Class<?> c, Type g, Annotation[] a, MediaType t) { return -1; }

    @Override
    public void writeTo(MetricRegistry metrics, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        try (Writer out = new OutputStreamWriter(entityStream)) {
            new MetricsWriter<Counter>(out, "counters", metrics.getCounters()).write();
            new MetricsWriter<Gauge>(out, "gauges", metrics.getGauges()).write();
            new MetricsWriter<Meter>(out, "meters", metrics.getMeters()).write();
            new MetricsWriter<Histogram>(out, "histograms", metrics.getHistograms()).write();
            new MetricsWriter<Timer>(out, "timers", metrics.getTimers()).write();
        }
    }

    @RequiredArgsConstructor
    private class MetricsWriter<T extends Metric> {
        private final Writer out;
        private final String name;
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        private final SortedMap<String, T> metrics;

        private String lastPath;

        public void write() {
            write(name);
            write(":");
            for (Map.Entry<String, T> entry : metrics.entrySet()) {
                String pad = writeKey(entry.getKey());
                writeValue(pad, entry.getValue());
            }
            write("\n");
        }

        private String writeKey(String key) {
            String pad = "\n  ";
            String path = "";
            for (String item : key.split("\\.")) {
                path += item + ".";
                if (lastPath == null || !lastPath.startsWith(path))
                    write(pad + item + ":");
                pad += "  ";
            }
            lastPath = path;
            return pad;
        }

        @SuppressWarnings("ChainOfInstanceofChecks") private void writeValue(String pad, T value) {
            Map<String, Function<T, Object>> attributes = new LinkedHashMap<>();
            if (value instanceof Counting)
                attributes.put("count", v -> ((Counting) v).getCount());
            if (value instanceof Gauge)
                attributes.put("value", v -> ((Gauge) v).getValue());
            if (value instanceof Metered) {
                attributes.put("mean_rate", v -> ((Metered) v).getMeanRate());
                attributes.put("m1_rate", v -> ((Metered) v).getOneMinuteRate());
                attributes.put("m5_rate", v -> ((Metered) v).getFiveMinuteRate());
                attributes.put("m15_rate", v -> ((Metered) v).getFifteenMinuteRate());
            }
            if (value instanceof Sampling) {
                attributes.put("max", v -> ((Sampling) v).getSnapshot().getMax());
                attributes.put("mean", v -> ((Sampling) v).getSnapshot().getMean());
                attributes.put("min", v -> ((Sampling) v).getSnapshot().getMin());
                attributes.put("stddev", v -> ((Sampling) v).getSnapshot().getStdDev());
                attributes.put("p50", v -> ((Sampling) v).getSnapshot().getMedian());
                attributes.put("p75", v -> ((Sampling) v).getSnapshot().get75thPercentile());
                attributes.put("p95", v -> ((Sampling) v).getSnapshot().get95thPercentile());
                attributes.put("p98", v -> ((Sampling) v).getSnapshot().get98thPercentile());
                attributes.put("p99", v -> ((Sampling) v).getSnapshot().get99thPercentile());
                attributes.put("p999", v -> ((Sampling) v).getSnapshot().get999thPercentile());
            }

            switch (attributes.size()) {
            case 0:
                write(" " + value);
                break;
            case 1:
                write(" " + attributes.values().iterator().next().apply(value));
                break;
            default:
                attributes.forEach((n, f) -> write(pad + n + ": " + f.apply(value)));
                break;
            }
        }

        @SneakyThrows(IOException.class) private void write(String str) { out.write(str); }
    }
}
