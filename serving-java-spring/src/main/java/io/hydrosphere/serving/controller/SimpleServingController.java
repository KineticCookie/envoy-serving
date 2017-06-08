package io.hydrosphere.serving.controller;

import com.fasterxml.jackson.databind.JsonNode;
import io.hydrosphere.serving.service.StageExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
@RestController
@RequestMapping("/v1/serving")
public class SimpleServingController {

    @Autowired
    private StageExecutor emptyStageExecutor;

    @RequestMapping(method = RequestMethod.POST)
    public JsonNode serve(@RequestBody JsonNode jsonNode, HttpServletRequest httpRequest, HttpServletResponse response) {
        Enumeration<String> enumeration= httpRequest.getHeaderNames();
        Map<String, String> map=new HashMap<>();
        while (enumeration.hasMoreElements()){
            String n=enumeration.nextElement();
            map.put(n, httpRequest.getHeader(n));
            /*if("x-request-id".equals(n)){
                response.addHeader("x-request-id", httpRequest.getHeader(n));
            }
            if("x-b3-traceid".equals(n)){
                response.addHeader("x-b3-traceid", httpRequest.getHeader(n));
            }*/
        }
        System.err.println(map);

        return emptyStageExecutor.execute("", jsonNode);
    }
}
