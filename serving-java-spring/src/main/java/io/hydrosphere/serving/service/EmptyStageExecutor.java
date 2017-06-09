package io.hydrosphere.serving.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.hydrosphere.serving.config.ServingConfigurationProperties;
import io.hydrosphere.serving.config.SideCarConfig;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 */
public class EmptyStageExecutor implements StageExecutor {

    @Autowired
    private SideCarConfig.SideCarConfigurationProperties properties;

    @Autowired
    private ServingConfigurationProperties servingConfigurationProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public JsonNode execute(String action, JsonNode data) {
        JsonNode jsonNode = data.deepCopy();
        ArrayNode arrayNode = (ArrayNode) jsonNode;
        ObjectNode objectNode = new ObjectNode(objectMapper.getNodeFactory());
        objectNode.put(servingConfigurationProperties.getServiceName(), properties.getServiceId());
        arrayNode.add(objectNode);

        return arrayNode;
    }
}
