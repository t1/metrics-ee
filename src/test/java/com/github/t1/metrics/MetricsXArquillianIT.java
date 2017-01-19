package com.github.t1.metrics;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

import javax.ws.rs.client.*;
import java.net.URI;

/** The 'X' in the name makes this IT run last, so the port 8080 is not blocked for the {@link MetricsTestToolsIT} */
@RunWith(Arquillian.class)
public class MetricsXArquillianIT extends MetricsIT {
    @Deployment(testable = false)
    public static WebArchive deployable() { return MetricsIT.deployable(); }

    @ArquillianResource URI baseUri;

    @Override public WebTarget http() { return ClientBuilder.newClient().target(baseUri); }
}
