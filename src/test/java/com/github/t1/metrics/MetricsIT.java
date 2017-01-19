package com.github.t1.metrics;

import com.github.t1.testtools.*;
import lombok.extern.slf4j.Slf4j;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.*;

import javax.ws.rs.client.WebTarget;

import static com.github.t1.metrics.MetricsYamlMessageBodyWriter.*;
import static javax.ws.rs.core.MediaType.*;
import static org.assertj.core.api.Assertions.*;

@Slf4j
public abstract class MetricsIT {
    public static WebArchive deployable() {
        return new WebArchiveBuilder("metrics-test.war")
                .with(RestApplication.class, MockHealthCheck.class)
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

    @Test
    public void shouldGetHealthChecks() {
        WebTarget request = http().path("/-healthchecks");

        log.debug("GET {}", request.getUri());

        String response = request.request(APPLICATION_JSON_TYPE).get(String.class);

        log.debug(" -> {}", response);

        assertThat(response).isEqualTo(""
                + "{\"" + MockHealthCheck.class.getName() + "\":"
                + "{\"healthy\":true,\"message\":\"healthy:1\",\"error\":null}}");
    }

    @Test
    public void shouldGetMetrics() throws Exception {
        WebTarget request = http().path("/-metrics");

        log.debug("GET {}", request.getUri());

        assertThat(request.request(APPLICATION_YAML_TYPE).get(String.class).replace("\"", "'"))
                .contains(""
                        + "jvm:\n"
                        + "  buffer-pools:\n"
                        + "    direct:\n"
                        + "      capacity: ")
                .contains(""
                        + "com:\n"
                        + "  github:\n"
                        + "    t1:\n"
                        + "      metrics:\n"
                        + "        MockHealthCheck: 1.0\n")
                .contains(""
                        + "resources:\n"
                        + "  /-healthchecks:\n"
                        + "    GET:\n"
                        + "      count: ");
    }
}
