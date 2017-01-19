package com.github.t1.metrics;

import com.codahale.metrics.*;
import com.codahale.metrics.health.*;
import com.github.t1.testtools.*;
import io.dropwizard.testing.junit.DropwizardClientRule;
import lombok.extern.java.Log;
import org.glassfish.hk2.api.*;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.logging.LoggingFeature;
import org.junit.ClassRule;

import javax.enterprise.inject.Instance;
import javax.ws.rs.client.*;

import static com.github.t1.log.LogLevel.*;
import static org.glassfish.jersey.logging.LoggingFeature.Verbosity.*;

@Log
public class MetricsDropwizardIT extends MetricsIT {
    private static CdiBinding cdiBinding = new CdiBinding();

    private static MockHealthCheck mockHealthCheck = new MockHealthCheck();

    public static @ClassRule LoggerMemento loggerMemento = new LoggerMemento()
            .with("com.github.t1.metrics", DEBUG);

    public static @ClassRule DropwizardClientRule service = new DropwizardClientRule(
            new LoggingFeature(log, PAYLOAD_ANY),

            MetricsBoundary.class,
            JaxRsBinding.class,
            cdiBinding,
            MetricsYamlMessageBodyWriter.class,
            binding()
    ) {
        @Override protected void before() throws Throwable {
            super.before();
            cdiBinding.init();
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
                bind(new FactoryInstance<>(new Factory<HealthCheck>() {
                    @Override public HealthCheck provide() { return mockHealthCheck; }

                    @Override public void dispose(HealthCheck instance) { }
                })).to(new TypeLiteral<Instance<HealthCheck>>() {});
                bind(new FactoryInstance<>(new Factory<Gauge>() {
                    @Override public Gauge provide() { return mockHealthCheck; }

                    @Override public void dispose(Gauge instance) { }
                })).to(new TypeLiteral<Instance<Gauge>>() {});
            }
        };
    }

    @Override public WebTarget http() { return ClientBuilder.newClient().target(service.baseUri()); }
}
