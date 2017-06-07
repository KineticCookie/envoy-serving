package io.hydrosphere.serving.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.hydrosphere.serving.proto.ServingPipeline;
import io.hydrosphere.serving.service.EndpointDefinition;
import io.hydrosphere.serving.service.EndpointService;
import io.hydrosphere.serving.service.GRPCGatewayServiceImpl;
import io.hydrosphere.serving.service.HTTPGatewayServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

/**
 *
 */
@RestController
@RequestMapping("/api/v1/serve")
public class ServingController {

    @Autowired
    private EndpointService endpointService;

    @Autowired
    private GRPCGatewayServiceImpl grpcGatewayService;

    @Autowired
    private HTTPGatewayServiceImpl httpGatewayService;


    @RequestMapping(value = "/{endpoint}", method = RequestMethod.POST)
    public DeferredResult<JsonNode> execute(@RequestBody JsonNode jsonNode, @PathVariable String endpoint) {
        EndpointDefinition definition = endpointService.endpointDefinition(endpoint);
        if (definition == null) {
            throw new WrongEndpointNameException();
        }
        if ("http".equals(definition.getTransportType())) {
            return httpGatewayService.execute(definition, jsonNode);
        } else {
            ServingPipeline servingPipeline = endpointService.getPipeline(endpoint, jsonNode);
            return grpcGatewayService.sendToMesh(servingPipeline);
        }
    }
}
