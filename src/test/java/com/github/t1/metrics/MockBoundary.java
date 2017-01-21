package com.github.t1.metrics;

import com.codahale.metrics.*;

import javax.inject.Inject;
import javax.ws.rs.*;

import static java.util.concurrent.TimeUnit.*;

@Path("/mock")
public class MockBoundary {
    @Inject Counter counter;
    @Inject Meter meter;
    @Inject Timer timer;

    @POST
    @Path("/counter")
    public long count() {
        counter.inc();
        return counter.getCount();
    }

    @POST
    @Path("/meter")
    public Meter meter() {
        meter.mark();
        return meter;
    }

    @POST
    @Path("/timer")
    public Timer timer() {
        timer.update(12, MINUTES);
        return timer;
    }
}
