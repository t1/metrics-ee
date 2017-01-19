package com.github.t1.metrics;

public class MockHealthCheck extends GaugedHealthCheck {
    private int counter = 0;

    @Override protected Result check() throws Exception {
        return (++counter % 5 == 0) ? Result.unhealthy("failed:" + counter) : Result.healthy("healthy:" + counter);
    }
}
