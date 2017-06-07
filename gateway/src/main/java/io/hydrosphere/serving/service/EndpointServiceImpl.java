package io.hydrosphere.serving.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import io.hydrosphere.serving.config.SideCarConfig;
import io.hydrosphere.serving.proto.ServingPipeline;
import io.hydrosphere.serving.proto.Stage;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 *
 */
@Service
public class EndpointServiceImpl implements EndpointService {
    private final ObjectMapper objectMapper = new ObjectMapper();


    private final SideCarConfig.SideCarConfigurationProperties sideCarConfigurationProperties;

    public EndpointServiceImpl(SideCarConfig.SideCarConfigurationProperties sideCarConfigurationProperties) {
        this.sideCarConfigurationProperties = sideCarConfigurationProperties;
    }

    @Override
    public ServingPipeline getPipeline(String endpoint, JsonNode jsonNode) {
        ServingPipeline servingPipeline;
        List<Stage> stages = Collections.singletonList(Stage.newBuilder()
                .setAction("action")
                .setDestination("serving-model1")
                .setType(Stage.StageType.SERVE)
                .build());
        try {
            servingPipeline = ServingPipeline.newBuilder()
                    .setStartTime(System.currentTimeMillis())
                    .setRequestId(UUID.randomUUID().toString())//TODO BAD UUID generator
                    .setGatewayDestination(this.sideCarConfigurationProperties.getServiceId())
                    .addAllStages(stages)
                    .setData(ByteString.copyFrom(objectMapper.writeValueAsBytes(jsonNode)))
                    .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return servingPipeline;
    }

    @Override
    public void create(EndpointDefinition definition) {
        
    }

    @Override
    public EndpointDefinition getDefinition(String name) {
        return null;
    }

    @Override
    public void delete(String name) {

    }

    @Override
    public void updateDefinition(EndpointDefinition definition) {

    }

    @Override
    public List<EndpointDefinition> definitions() {
        return null;
    }
}
