# Metrics-EE [ ![Download](https://api.bintray.com/packages/t1/javaee-helpers/metrics-ee/images/download.svg) ](https://bintray.com/t1/javaee-helpers/metrics-ee/_latestVersion)

Binds [Dropwizard Metrics](https://metrics.dropwizard.io) to Java EE,
i.e. it times all JAX-RS endpoints called and all clients calling,
and exposes all metrics and health checks at:

`https://<your-host>/<your-app>/-metrics`

and

`https://<your-host>/<your-app>/-healthchecks`

You can also add custom metrics and health checks – just implement the `Gauge` or `HealthCheck` interfaces,
or extend `GaugedHealthCheck` to have a health check that provides the ratio as gauge.
Or `@Inject` dependent instances of `Counter`, `Meter`, or `Timer`.
Or `@Inject` the `MetricsRegistry` or `HealthCheckRegistry` for full control.

In a Java EE 7+ `war`, all you need to add is this dependency (and add a `beans.xml`, if you don't have one, yet):

    <dependency>
        <groupId>com.github.t1</groupId>
        <artifactId>metrics-ee</artifactId>
        <version>${version}</version>
    </dependency>

It's not on maven central, so you'll have to grab it from [bintray](https://bintray.com/t1/javaee-helpers/metrics-ee/view).
