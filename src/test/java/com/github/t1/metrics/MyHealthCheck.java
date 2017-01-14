package com.github.t1.metrics;

import java.util.Random;

public class MyHealthCheck extends GaugedHealthCheck {
    private final Random random = new Random();

    @Override protected Result check() throws Exception {
        return (random.nextBoolean()) ? Result.healthy() : Result.unhealthy("failed");
    }
}
