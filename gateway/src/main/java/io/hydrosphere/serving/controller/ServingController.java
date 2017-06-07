package io.hydrosphere.serving.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.hydrosphere.serving.proto.ServingPipeline;
import io.hydrosphere.serving.service.EndpointService;
import io.hydrosphere.serving.service.GRPCGatewayServiceImpl;
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


    @RequestMapping(value = "/{endpoint}", method = RequestMethod.POST)
    public DeferredResult<JsonNode> execute(@RequestBody JsonNode jsonNode, @PathVariable String endpoint) {
        ServingPipeline servingPipeline = endpointService.getPipeline(endpoint, jsonNode);
        if (servingPipeline == null) {
            throw new WrongEndpointNameException();
        }

        return grpcGatewayService.sendToMesh(servingPipeline);
    }
}
