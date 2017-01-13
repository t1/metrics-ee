package com.github.t1.metrics;

import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.ext.*;

import static java.util.concurrent.TimeUnit.*;

@Provider
public class JacksonMagic implements ContextResolver<ObjectMapper> {
    private final ObjectMapper mapper;

    public JacksonMagic() {
        mapper = new ObjectMapper();
        mapper.registerModule(new MetricsModule(SECONDS, SECONDS, false));
    }

    @Override
    public ObjectMapper getContext(Class<?> type) { return mapper; }
}
