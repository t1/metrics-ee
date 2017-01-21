package com.github.t1.metrics;

import com.github.t1.testtools.*;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.*;
import org.junit.runner.RunWith;

import javax.ws.rs.client.*;

import static com.github.t1.log.LogLevel.*;

@RunWith(OrderedJUnitRunner.class)
public class MetricsTestToolsIT extends MetricsIT {
    @ClassRule public static WildflyContainerTestRule container = new WildflyContainerTestRule("10.1.0.Final")
            .withLogger("org.apache.http.headers", DEBUG)
            // .withLogger("org.apache.http.wire", DEBUG)
            .withLogger("org.jboss.weld", DEBUG)
            .withLogger("com.github.t1", DEBUG);

    private static WebArchive deployable;

    @BeforeClass
    public static void deploy() throws Exception {
        deployable = deployable();
        container.deploy(deployable);
    }

    @Override public WebTarget http() {
        return ClientBuilder.newClient().target(container.baseUri()).path(deployable.getName().replace(".war", ""));
    }
}
