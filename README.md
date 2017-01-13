# Metrics-EE [ ![Download](https://api.bintray.com/packages/t1/javaee-helpers/metrics-ee/images/download.svg) ](https://bintray.com/t1/javaee-helpers/metrics-ee/_latestVersion)

Binds [Metrics](https://metrics.dropwizard.io) to Java EE, times all JAX-RS endpoints and client calls,
and exposes all metrics at `https://<your-host>/<your-app>/-metrics`.
You can also add custom metrics and health checks.

In a Java EE 6+ `war`, all you need to add is this dependency (and add a `beans.xml`, if you don't have one, yet):

    <dependency>
        <groupId>com.github.t1</groupId>
        <artifactId>metrics-ee</artifactId>
        <version>${version}</version>
    </dependency>

It's not on maven central, so you'll have to grab it from [bintray](https://bintray.com/t1/javaee-helpers/metrics-ee/view).
