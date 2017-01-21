package com.github.t1.metrics;

import com.github.t1.testtools.*;
import lombok.extern.slf4j.Slf4j;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.*;

import javax.ws.rs.client.*;
import javax.ws.rs.core.Form;

import static com.github.t1.metrics.MetricsYamlMessageBodyWriter.*;
import static javax.ws.rs.core.MediaType.*;
import static org.assertj.core.api.Assertions.*;

@Slf4j
public abstract class MetricsIT {
    public static WebArchive deployable() {
        return new WebArchiveBuilder("metrics-test.war")
                .with(RestApplication.class, MockHealthCheck.class, MockGaugedHealthCheck.class, MockBoundary.class)
                .withBeansXml()
                .library("io.dropwizard.metrics", "metrics-core")
                .library("io.dropwizard.metrics", "metrics-healthchecks")
                .library("io.dropwizard.metrics", "metrics-json")
                .library("io.dropwizard.metrics", "metrics-jvm")
                .addPackageAndDependencies(Metrics.class)
                .print()
                .build();
    }

    @Rule public TestLoggerRule logger = new TestLoggerRule();

    public abstract WebTarget http();

    private String POST(WebTarget request) {
        return request.request(APPLICATION_JSON_TYPE).post(Entity.form(new Form())).readEntity(String.class);
    }

    @Test
    public void shouldGetHealthChecks() {
        WebTarget request = http().path("/-healthchecks");

        String response = request.request(APPLICATION_JSON_TYPE).get(String.class);

        assertThat(response).isEqualTo(""
                + "{"
                + "\"" + MockGaugedHealthCheck.class.getName() + "\":"
                + "{\"healthy\":true,\"message\":\"healthy:1\",\"error\":null},"
                + "\"" + MockHealthCheck.class.getName() + "\":"
                + "{\"healthy\":true,\"message\":\"healthy:1\",\"error\":null}}"
        );
    }

    @Test
    public void shouldPostCounter() throws Exception {
        WebTarget request = http().path("/mock/counter");

        String response = POST(request);

        assertThat(response).isEqualTo("1");
    }

    @Test
    public void shouldPostMeter() throws Exception {
        WebTarget request = http().path("/mock/meter");

        String response = POST(request);

        assertThat(response.replace("\"", "\'"))
                .startsWith("{'count':1,'m15_rate':0.0,'m1_rate':0.0,'m5_rate':0.0,'mean_rate':")
                .endsWith(",'units':'events/second'}");
    }

    @Test
    public void shouldPostTimer() throws Exception {
        WebTarget request = http().path("/mock/timer");

        String response = POST(request);

        assertThat(response.replace("\"", "\'"))
                .startsWith("{'count':1,'max':")
                .contains(",'p50':")
                .contains("'stddev':0.0,'m15_rate':0.0,'m1_rate':0.0,'m5_rate':0.0,")
                .contains("'mean_rate':")
                .endsWith(",'duration_units':'seconds','rate_units':'calls/second'}");
    }

    @Test
    @InSequence(Integer.MAX_VALUE)
    public void shouldGetMetrics() throws Exception {
        WebTarget request = http().path("/-metrics");

        String response = request.request(APPLICATION_YAML_TYPE).get(String.class);

        log.info("--->\n{}", response);
        assertThat(response)
                .contains("\n"
                        + "jvm:\n"
                        + "  buffer-pools:\n"
                        + "    direct:\n"
                        + "      capacity: ")
                .contains("\n"
                        + "com:\n"
                        + "  github:\n"
                        + "    t1:\n"
                        + "      metrics:\n")
                .contains("\n"
                        + "        MockBoundary:\n"
                        + "          counter: 1\n"
                        + "          meter:\n"
                        + "            count: 1\n")
                .contains("\n"
                        + "        MockGaugedHealthCheck: 1.0\n")
                .contains("\n"
                        + "resources:\n"
                        + "  /-healthchecks:\n"
                        + "    GET:\n"
                        + "      200:\n"
                        + "        count: ");
    }
}
