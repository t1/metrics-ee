package com.github.t1.metrics;

import com.codahale.metrics.*;
import com.codahale.metrics.health.*;
import com.codahale.metrics.jvm.*;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.*;
import javax.enterprise.inject.*;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.*;
import java.lang.reflect.Member;

import static java.lang.management.ManagementFactory.*;
import static java.util.concurrent.TimeUnit.*;

@Slf4j
@Singleton
public class CdiBinding {
    final MetricRegistry metrics = new MetricRegistry();
    final HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();

    @Inject
    Instance<HealthCheck> healthChecks;

    @Inject
    Instance<Gauge> gauges;

    JmxReporter jmxReporter;

    @PostConstruct
    public void init() {
        log.debug("start jmx reporter");
        jmxReporter = JmxReporter.forRegistry(metrics).build();
        jmxReporter.start();

        for (HealthCheck healthCheck : healthChecks) {
            String name = healthCheck.getClass().getName();
            log.debug("register health check: {}", name);
            healthCheckRegistry.register(name, healthCheck);
            if (healthCheck instanceof Gauge) {
                log.debug("register gauge: {}", name);
                metrics.register(name, (Gauge) healthCheck);
            }
        }

        for (Gauge<?> gauge : gauges) {
            String name = gauge.getClass().getName();
            if (gauge instanceof HealthCheck) {
                log.debug("gauge already registered as healthcheck: {}", name);
            } else {
                log.debug("register gauge: {}", name);
                metrics.register(name, gauge);
            }
        }

        log.debug("register jvm gauges");
        metrics.register("jvm", new JvmAttributeGaugeSet());
        metrics.register("jvm.class-loader", new ClassLoadingGaugeSet());
        metrics.register("jvm.buffer-pools", new BufferPoolMetricSet(getPlatformMBeanServer()));
        metrics.register("jvm.gc", new GarbageCollectorMetricSet());
        metrics.register("jvm.memory", new MemoryUsageGaugeSet());
        metrics.register("jvm.threads", new CachedThreadStatesGaugeSet(1, MINUTES));
    }

    @PreDestroy public void destroy() {
        if (jmxReporter != null) {
            log.debug("stop jmx reporter");
            jmxReporter.close();
        }
    }

    @Produces public MetricRegistry produceMetricRegistry() { return metrics; }

    @Produces public HealthCheckRegistry produceHealthCheckRegistry() { return healthCheckRegistry; }

    @Produces
    public Counter produceCounter(InjectionPoint injectionPoint) { return metrics.counter(name(injectionPoint)); }

    @Produces
    public Meter produceMeter(InjectionPoint injectionPoint) { return metrics.meter(name(injectionPoint)); }

    @Produces
    public Timer produceTimer(InjectionPoint injectionPoint) { return metrics.timer(name(injectionPoint)); }

    private String name(InjectionPoint injectionPoint) {
        Member member = injectionPoint.getMember();
        Class<?> beanClass = member.getDeclaringClass();
        String counterName = member.getName();
        return MetricRegistry.name(beanClass, counterName);
    }
}
