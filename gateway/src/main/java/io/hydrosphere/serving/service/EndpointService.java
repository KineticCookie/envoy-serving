package io.hydrosphere.serving.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.hydrosphere.serving.proto.ServingPipeline;

import java.util.List;

/**
 *
 */
public interface EndpointService {
    ServingPipeline getPipeline(String endpoint, JsonNode jsonNode);

    void create(EndpointDefinition definition);

    EndpointDefinition getDefinition(String name);

    void delete(String name);

    void updateDefinition(EndpointDefinition definition);

    List<EndpointDefinition> definitions();
}
