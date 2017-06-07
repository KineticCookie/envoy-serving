package io.hydrosphere.serving.service;

import lombok.Data;

import java.util.List;

/**
 *
 */
@Data
public class EndpointDefinition {
    private String name;

    private List<String> chain;
}
