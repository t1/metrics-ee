package com.github.t1.metrics;

import com.codahale.metrics.Clock;

import java.time.Instant;
import java.time.temporal.TemporalUnit;

public class MockClock extends Clock {
    private static final int NANOS_PER_MILLI = 1_000_000;

    private Instant time = Instant.now();

    @Override public long getTime() { return time.toEpochMilli(); }

    @Override public long getTick() { return time.toEpochMilli() * NANOS_PER_MILLI + time.getNano(); }

    public void plus(long delta, TemporalUnit unit) { this.time = this.time.plus(delta, unit); }
}
