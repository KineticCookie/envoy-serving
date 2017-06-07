package io.hydrosphere.serving.controller;

import io.grpc.stub.StreamObserver;
import io.hydrosphere.serving.config.SideCarConfig;
import io.hydrosphere.serving.proto.HealthRequest;
import io.hydrosphere.serving.proto.HealthResponse;
import io.hydrosphere.serving.proto.HealthServiceGrpc;
import io.hydrosphere.serving.proto.HealthStatus;
import io.hydrosphere.serving.service.AuthorityReplacerInterceptor;
import io.hydrosphere.serving.service.MeshManagerService;
import io.hydrosphere.serving.service.Service;
import io.hydrosphere.serving.service.ServiceStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.DeferredResult;

/**
 *
 */
@Controller
@RequestMapping("/v1/health")
public class HealthCheckController {

    @Autowired
    HealthServiceGrpc.HealthServiceStub healthServiceStub;

    @Autowired
    MeshManagerService managerService;

    @Autowired
    SideCarConfig.SideCarConfigurationProperties sideCarConfigurationProperties;

    @RequestMapping("/{serviceId}")
    @ResponseBody
    public DeferredResult<String> heath(@PathVariable String serviceId) {
        Service service = managerService.getService(serviceId);
        if (service == null) {
            throw new RuntimeException("Can't find service=" + serviceId);
        }
        DeferredResult<String> result = new DeferredResult<>();

        if (sideCarConfigurationProperties.getServiceId().equals(service.getServiceId())) {
            //TODO wrong place to do
            service.setLastKnownStatus(ServiceStatus.UP);
            result.setResult("OK");
        } else {
            healthServiceStub.withOption(AuthorityReplacerInterceptor.DESTINATION_KEY, serviceId)
                    .health(HealthRequest.newBuilder().build(), new StreamObserver<HealthResponse>() {
                        @Override
                        public void onNext(HealthResponse value) {
                            if (value.getStatus() == HealthStatus.UP) {
                                //TODO wrong place to do
                                service.setLastKnownStatus(ServiceStatus.UP);
                                result.setResult("OK");
                            } else {
                                //TODO wrong place to do
                                service.setLastKnownStatus(ServiceStatus.DOWN);
                                result.setErrorResult(String.valueOf(value));
                            }
                        }

                        @Override
                        public void onError(Throwable t) {
                            //TODO wrong place to do
                            service.setLastKnownStatus(ServiceStatus.DOWN);
                            result.setErrorResult(t);
                        }

                        @Override
                        public void onCompleted() {
                        }
                    });
        }
        return result;
    }
}
