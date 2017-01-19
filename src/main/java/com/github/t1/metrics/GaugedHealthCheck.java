package com.github.t1.metrics;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.RatioGauge.Ratio;
import com.codahale.metrics.health.HealthCheck;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class GaugedHealthCheck extends HealthCheck implements Gauge<Double> {

    private int total, healthy;

    @Override public Double getValue() { return Ratio.of(healthy, total).getValue(); }

    @Override public Result execute() {
        Result result = super.execute();

        ++total;
        if (result.isHealthy())
            ++healthy;

        log.debug("{}:{} -> {}/{}", getClass().getName(), result, healthy, total);

        return result;
    }
}
