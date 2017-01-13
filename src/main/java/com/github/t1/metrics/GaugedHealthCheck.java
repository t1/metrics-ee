package com.github.t1.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.RatioGauge.Ratio;
import com.codahale.metrics.health.HealthCheck;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

public abstract class GaugedHealthCheck extends HealthCheck {
    @Inject Metrics metrics;

    private int total, healthy;

    @PostConstruct public void init() { metrics.register(gaugeName(), this::ratio); }

    protected String gaugeName() { return MetricRegistry.name(getClass(), "health-ratio"); }

    protected double ratio() { return Ratio.of(healthy, total).getValue(); }

    @Override public Result execute() {
        Result result = super.execute();

        ++total;
        if (result.isHealthy())
            ++healthy;
        return result;
    }
}
