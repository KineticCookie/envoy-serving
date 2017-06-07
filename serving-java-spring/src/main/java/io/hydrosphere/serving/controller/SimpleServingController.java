package io.hydrosphere.serving.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.hydrosphere.serving.service.StageExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 */
@RestController
@RequestMapping("/v1/serving")
public class SimpleServingController {

    @Autowired
    private StageExecutor emptyStageExecutor;

    @RequestMapping(method = RequestMethod.POST)
    public JsonNode serve(@RequestBody JsonNode jsonNode) {
        return emptyStageExecutor.execute("", jsonNode);
    }
}
