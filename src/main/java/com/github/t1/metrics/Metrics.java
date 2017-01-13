package com.github.t1.metrics;

import com.codahale.metrics.*;

import javax.inject.Inject;

public class Metrics {
    @Inject MetricRegistry registry;

    public RatioGauge register(String name, RatioGauge ratioGauge) { return register(name, (Metric) ratioGauge); }

    public <T> Gauge<T> register(String name, Gauge<T> gauge) { return register(name, (Metric) gauge); }

    private <U extends Metric> U register(String name, Metric metric) {
        if (!registry.getMetrics().containsKey(name))
            registry.register(name, metric);
        //noinspection unchecked
        return (U) metric;
    }
}
