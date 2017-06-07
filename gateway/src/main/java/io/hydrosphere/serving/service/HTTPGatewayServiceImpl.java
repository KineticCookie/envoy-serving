package io.hydrosphere.serving.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hydrosphere.serving.config.SideCarConfig;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 *
 */
@Service
public class HTTPGatewayServiceImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(HTTPGatewayServiceImpl.class);

    private final HttpClient httpClient = new HttpClient();

    private final SideCarConfig.SideCarConfigurationProperties properties;

    private final ObjectMapper objectMapper=new ObjectMapper();

    public HTTPGatewayServiceImpl(SideCarConfig.SideCarConfigurationProperties properties) {
        try {
            httpClient.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.properties=properties;
    }

    public DeferredResult<JsonNode> execute(EndpointDefinition definition, JsonNode jsonNode) {
        DeferredResult<JsonNode> r = new DeferredResult<>();
        try {
            r.setResult(sendAll(definition, jsonNode));
        } catch (Exception e) {
            r.setErrorResult(e);
        }
        return r;
    }

    public JsonNode sendAll(EndpointDefinition definition, JsonNode jsonNode) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        JsonNode result = jsonNode;
        for (String s : definition.getChain()) {
            ContentResponse response = httpClient.newRequest("http://"+properties.getHost()+":" + properties.getHttpPort() + s.substring(s.indexOf("/")))
                    .method(HttpMethod.POST)
                    .scheme("http")
                    .header(HttpHeader.HOST, "http-"+s.substring(0, s.indexOf("/")))
                    .content(new StringContentProvider(objectMapper.writeValueAsString(jsonNode)), "application/json")
                    .send();

            LOGGER.info("RES: {}, {}", response.getStatus(), response.getReason());

            result=objectMapper.readTree(response.getContent());


            jsonNode = result;
        }
        return result;

    }
}
