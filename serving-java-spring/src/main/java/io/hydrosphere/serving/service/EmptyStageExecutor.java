package io.hydrosphere.serving.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.hydrosphere.serving.config.ServingConfigurationProperties;
import io.hydrosphere.serving.config.SideCarConfig;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 */
public class EmptyStageExecutor implements StageExecutor {

    @Autowired
    private SideCarConfig.SideCarConfigurationProperties properties;

    @Override
    public JsonNode execute(String action, JsonNode data) {
        JsonNode jsonNode = data.deepCopy();
        ObjectNode objectNode = (ObjectNode) jsonNode;
        objectNode.put(properties.getServiceId(),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return objectNode;
    }
}
