package com.github.t1.metrics;

import com.codahale.metrics.*;
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
public class MetricsYamlMessageBodyWriter implements MessageBodyWriter<SortedMap<String, Metric>> {
    public static final String APPLICATION_YAML = "application/yaml";
    public static final MediaType APPLICATION_YAML_TYPE = MediaType.valueOf(APPLICATION_YAML);

    public static final GenericType METRICS_MAP = new GenericType<SortedMap<String, Metric>>() {};


    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return METRICS_MAP.getType().equals(genericType) && mediaType.isCompatible(APPLICATION_YAML_TYPE);
    }

    @Override
    public long getSize(SortedMap<String, Metric> r, Class<?> c, Type g, Annotation[] a, MediaType t) { return -1; }

    @Override
    public void writeTo(SortedMap<String, Metric> metrics, Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        try (Writer out = new OutputStreamWriter(entityStream)) {
            new MetricsWriter(out).write(metrics);
        }
    }

    @RequiredArgsConstructor
    private class MetricsWriter {
        private final Writer out;

        private String lastPath;

        public void write(SortedMap<String, Metric> metrics) {
            for (Map.Entry<String, Metric> entry : metrics.entrySet()) {
                String padding = writeKey(entry.getKey());
                writeValue(padding, entry.getValue());
            }
            write("\n");
        }

        private String writeKey(String key) {
            String padding = "\n";
            String path = "";
            String[] slashItems = key.split("/", 2);
            for (String dotItem : slashItems[0].split("\\.")) {
                path += dotItem + ".";
                if (lastPath == null || !lastPath.startsWith(path))
                    write(padding + dotItem + ":");
                padding += "  ";
            }
            if (slashItems.length == 2) {
                String[] pipeItems = slashItems[1].split("\\|", 2);
                for (String slashItem : pipeItems[0].split("/")) {
                    path += slashItem + "|";
                    if (lastPath == null || !lastPath.startsWith(path))
                        write(padding + "/" + slashItem + ":");
                    padding += "  ";
                }
                if (pipeItems.length == 2) {
                    for (String pipeItem : pipeItems[1].split("\\|")) {
                        path += pipeItem + "|";
                        if (lastPath == null || !lastPath.startsWith(path))
                            write(padding + pipeItem + ":");
                        padding += "  ";
                    }
                }
            }
            lastPath = path;
            return padding;
        }

        private void writeValue(String padding, Metric value) {
            writeAttributes(padding, value, getAttributes(value));
        }

        @SuppressWarnings("ChainOfInstanceofChecks")
        private Map<String, Function<Metric, Object>> getAttributes(Metric value) {
            Map<String, Function<Metric, Object>> attributes = new LinkedHashMap<>();
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
                attributes.put("min", v -> ((Sampling) v).getSnapshot().getMin());
                attributes.put("mean", v -> ((Sampling) v).getSnapshot().getMean());
                attributes.put("max", v -> ((Sampling) v).getSnapshot().getMax());
                attributes.put("stddev", v -> ((Sampling) v).getSnapshot().getStdDev());
                attributes.put("p50", v -> ((Sampling) v).getSnapshot().getMedian());
                attributes.put("p75", v -> ((Sampling) v).getSnapshot().get75thPercentile());
                attributes.put("p95", v -> ((Sampling) v).getSnapshot().get95thPercentile());
                attributes.put("p98", v -> ((Sampling) v).getSnapshot().get98thPercentile());
                attributes.put("p99", v -> ((Sampling) v).getSnapshot().get99thPercentile());
                attributes.put("p999", v -> ((Sampling) v).getSnapshot().get999thPercentile());
            }
            return attributes;
        }

        private void writeAttributes(String padding, Metric value, Map<String, Function<Metric, Object>> attributes) {
            switch (attributes.size()) {
            case 0:
                write(" " + value.toString());
                break;
            case 1:
                write(" " + attributes.values().iterator().next().apply(value));
                break;
            default:
                attributes.forEach((n, f) -> write(padding + n + ": " + f.apply(value)));
                break;
            }
        }

        @SneakyThrows(IOException.class) private void write(String str) { out.write(str); }
    }
}
