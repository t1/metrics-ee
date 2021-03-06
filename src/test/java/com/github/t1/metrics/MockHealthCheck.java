package com.github.t1.metrics;

import com.codahale.metrics.health.HealthCheck;

public class MockHealthCheck extends HealthCheck {
    private int counter = 0;

    @Override protected Result check() throws Exception {
        return (++counter % 5 == 0) ? Result.unhealthy("failed:" + counter) : Result.healthy("healthy:" + counter);
    }
}
