package com.github.t1.metrics;

import com.codahale.metrics.*;
import com.codahale.metrics.health.*;
import com.codahale.metrics.json.MetricsModule;
import com.github.t1.testtools.*;
import io.dropwizard.testing.junit.DropwizardClientRule;
import lombok.extern.java.Log;
import org.glassfish.hk2.api.*;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.logging.LoggingFeature;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import javax.enterprise.inject.Instance;
import javax.ws.rs.client.*;
import java.util.function.BiFunction;

import static com.github.t1.log.LogLevel.*;
import static java.util.concurrent.TimeUnit.*;
import static org.glassfish.jersey.logging.LoggingFeature.Verbosity.*;

@Log
@RunWith(OrderedJUnitRunner.class)
public class MetricsDropwizardIT extends MetricsIT {
    private static CdiBinding cdiBinding = new CdiBinding();

    private static MockHealthCheck mockHealthCheck = new MockHealthCheck();
    private static MockGaugedHealthCheck mockGaugedHealthCheck = new MockGaugedHealthCheck();

    public static @ClassRule LoggerMemento loggerMemento = new LoggerMemento()
            .with("com.github.t1.metrics", DEBUG);

    public static @ClassRule DropwizardClientRule service = new DropwizardClientRule(
            new LoggingFeature(log, PAYLOAD_ANY),

            MetricsBoundary.class,
            JaxRsBinding.class,
            cdiBinding,
            MetricsYamlMessageBodyWriter.class,
            MockBoundary.class,
            binding()
    ) {
        @Override protected void before() throws Throwable {
            super.before();
            cdiBinding.init();
            getObjectMapper().registerModule(new MetricsModule(SECONDS, SECONDS, false));
        }

        @Override protected void after() {
            cdiBinding.destroy();
            super.after();
        }
    };

    private static AbstractBinder binding() {
        return new AbstractBinder() {
            @Override
            protected void configure() {
                bind(cdiBinding.metrics).to(MetricRegistry.class);
                bind(cdiBinding.healthCheckRegistry).to(HealthCheckRegistry.class);
                bind(new MockInstance<>(mockHealthCheck, mockGaugedHealthCheck))
                        .to(new TypeLiteral<Instance<HealthCheck>>() {});
                bind(new MockInstance<>(mockGaugedHealthCheck))
                        .to(new TypeLiteral<Instance<Gauge>>() {});
                bindFactory(mockFactory((metrics, name) -> metrics.counter(name + ".counter"))).to(Counter.class);
                bindFactory(mockFactory((metrics, name) -> metrics.meter(name + ".meter"))).to(Meter.class);
                bindFactory(mockFactory((metrics, name) -> metrics.timer(name + ".timer"))).to(Timer.class);
            }
        };
    }

    private static <T> Factory<T> mockFactory(BiFunction<MetricRegistry, String, T> get) {
        return new Factory<T>() {
            @Override public T provide() {
                return get.apply(cdiBinding.metrics, MockBoundary.class.getName());
            }

            @Override public void dispose(T instance) {}
        };
    }

    @Override public WebTarget http() { return ClientBuilder.newClient().target(service.baseUri()); }
}
