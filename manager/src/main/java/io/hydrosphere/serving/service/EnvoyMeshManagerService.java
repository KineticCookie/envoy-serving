package io.hydrosphere.serving.service;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.NewService;
import io.hydrosphere.serving.config.ManagerConfig;
import io.hydrosphere.serving.config.SideCarConfig;
import io.hydrosphere.serving.service.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 */
@org.springframework.stereotype.Service
public class EnvoyMeshManagerService implements MeshManagerService {

    private final static Logger LOGGER = LoggerFactory.getLogger(EnvoyMeshManagerService.class);

    private final ConcurrentMap<String, Service> services = new ConcurrentHashMap<>();

    private final ConsulClient consulClient;

    private final ManagerConfig.ManagerConfigurationProperties managerConfigurationProperties;

    private final SideCarConfig.SideCarConfigurationProperties sideCarConfigurationProperties;

    @Autowired
    public EnvoyMeshManagerService(ConsulClient consulClient, ManagerConfig.ManagerConfigurationProperties managerConfigurationProperties, SideCarConfig.SideCarConfigurationProperties sideCarConfigurationProperties) {
        this.consulClient = consulClient;
        this.managerConfigurationProperties = managerConfigurationProperties;
        this.sideCarConfigurationProperties = sideCarConfigurationProperties;
    }

    private RouteHost create(String name, String cluster, String domain) {
        RouteHost routeHost = new RouteHost();
        routeHost.setDomains(Collections.singletonList(domain));
        routeHost.setName(name);
        routeHost.setRoutes(Collections.singletonList(new Route("/", cluster)));
        return routeHost;
    }

    @Override
    public RouteConfig routes(String configName, ServiceType cluster, String node) {
        LOGGER.debug("routes: {},{},{}", configName, cluster, node);
        List<RouteHost> routeHosts = new ArrayList<>();
        Map<String, RouteHost> serviceClusters = new HashMap<>();
        services.values().forEach((service) -> {
            if (service.isUseServiceGrpc() && configName.equals("grpc")) {
                String serviceName = getServiceName(service);
                serviceClusters.computeIfAbsent(serviceName, (s) -> create(s, s, s));
                routeHosts.add(create(service.getServiceId(), service.getServiceUUID(), service.getServiceId()));
            }
            if (service.isUseServiceHttp() && configName.equals("http")) {
                String serviceName = "http-" + getServiceName(service);
                serviceClusters.computeIfAbsent(serviceName, (s) -> create(s, s, s));
                routeHosts.add(create("http-" + service.getServiceId(), "http-" + service.getServiceUUID(), "http-" + service.getServiceId()));
            }
        });
        routeHosts.addAll(serviceClusters.values());
        if ("http".equals(configName)) {
            Service service = services.get(node);
            if (service!=null && service.isUseServiceHttp()) {
                routeHosts.add(create("all", "http-" + service.getServiceUUID(), "*"));
            }
        }
        RouteConfig routeConfig = new RouteConfig();
        routeConfig.setVirtualHosts(routeHosts);
        LOGGER.debug("result :{}", routeConfig);
        return routeConfig;
    }

    @Override
    public ClusterConfig clusters(ServiceType cluster, String node) {
        LOGGER.debug("clusters: {}, {}", cluster, node);
        ClusterConfig config = new ClusterConfig();
        List<Cluster> clusters = managerClusters(node);
        config.setClusters(clusters);
        LOGGER.debug("clusters: {}", config);
        return config;
    }

    private String getServiceName(Service service) {
        return service.getServiceType() + "-" + service.getServiceName();
    }

    /**
     * Because envoy cluster must be max 60 symbols
     *
     * @param service
     * @return
     */
    private String generateServiceUUID(Service service) {
        return UUID.nameUUIDFromBytes(getServiceName(service).getBytes()).toString();
    }

    private List<ClusterHost> getStaticHost(Service service, String forNode, boolean isHttp) {
        boolean sameNode = service.getServiceId().equals(forNode);
        StringBuilder builder = new StringBuilder("tcp://");
        if (sameNode) {
            builder.append("127.0.0.1");
        } else {
            builder.append(service.getIp());
        }

        builder.append(":");
        if (sameNode) {
            if (isHttp) {
                builder.append(service.getServiceHttpPort());
            } else {
                builder.append(service.getServiceGrpcPort());
            }
        } else {
            if (isHttp) {
                builder.append(service.getSideCarHttpPort());
            } else {
                builder.append(service.getSideCarGrpcPort());
            }
        }
        return Collections.singletonList(new ClusterHost(builder.toString()));
    }

    private List<Cluster> managerClusters(String node) {
        Service nodeService = services.get(node);
        if(nodeService==null){
            return Collections.emptyList();
        }
        String nodeServiceName = getServiceName(nodeService);


        List<Cluster> result = new ArrayList<>();
        Map<String, Cluster> serviceClusters = new HashMap<>();
        services.forEach((id, service) -> {
            Cluster.ClusterBuilder cluster = Cluster.builder()
                    .connectTimeoutMs(500)
                    .lbType("round_robin");

            String grpcServiceName = getServiceName(service);
            if (service.isUseServiceGrpc()) {
                cluster.serviceName(grpcServiceName)
                        .features("http2")
                        .name(grpcServiceName);
                if (nodeServiceName.equals(grpcServiceName)) {
                    serviceClusters.computeIfAbsent(grpcServiceName, (s) -> {
                        cluster.hosts(Collections.singletonList(new ClusterHost("tcp://127.0.0.1:" + service.getServiceGrpcPort())))
                                .type("static");
                        return cluster.build();
                    });
                } else {
                    serviceClusters.computeIfAbsent(grpcServiceName, (s) -> {
                        cluster.hosts(null)
                                .type("sds");
                        return cluster.build();
                    });
                }

                result.add(cluster.serviceName(null)
                        .name(service.getServiceUUID())
                        .type("static")
                        .hosts(getStaticHost(service, node, false))
                        .build());
            }
            String httpServiceName = "http-" + getServiceName(service);
            if (service.isUseServiceHttp()) {
                cluster.serviceName(httpServiceName)
                        .features(null)
                        .name(httpServiceName);

                if (nodeServiceName.equals(grpcServiceName)) {
                    serviceClusters.computeIfAbsent(httpServiceName, (s) -> {
                        cluster.hosts(Collections.singletonList(new ClusterHost("tcp://127.0.0.1:" + service.getServiceHttpPort())))
                                .type("static");
                        return cluster.build();
                    });
                } else {
                    serviceClusters.computeIfAbsent(httpServiceName, (s) -> {
                        cluster.hosts(null)
                                .type("sds");
                        return cluster.build();
                    });
                }

                result.add(cluster.serviceName(null)
                        .name("http-" + service.getServiceUUID())
                        .type("static")
                        .hosts(getStaticHost(service, node, true))
                        .build());
            }
        });
        result.addAll(serviceClusters.values());
        return result;
    }

    @Override
    public ServiceConfig services(String serviceName) {
        LOGGER.debug("services: {}", serviceName);
        int indexFrom = 0;
        boolean isHttp = false;
        if (serviceName.startsWith("http-")) {
            indexFrom = "http-".length();
            isHttp = true;
        }
        int indexName = serviceName.indexOf("-", indexFrom + 1);
        ServiceType serviceType = ServiceType.valueOf(serviceName.substring(indexFrom, indexName));
        String name = serviceName.substring(indexName + 1);

        List<ServiceHost> hosts = new ArrayList<>();
        boolean check = isHttp;
        LOGGER.debug("Current services: {}",services);
        services.values().stream()
                .filter(p -> p.getServiceType() == serviceType)
                .filter(p -> p.getLastKnownStatus() == ServiceStatus.UP)
                .filter(p -> p.getServiceName().equals(name))
                .forEach(p -> {
                    ServiceHost host = new ServiceHost();
                    if (check) {
                        host.setPort(p.getSideCarHttpPort());
                    } else {
                        host.setPort(p.getSideCarGrpcPort());
                    }
                    host.setIpAddress(p.getIp());
                    hosts.add(host);
                });
        ServiceConfig config = new ServiceConfig();
        config.setHosts(hosts);
        LOGGER.debug("services result: {}", config);
        return config;
    }

    @Override
    public void unregisterService(String serviceId) {
        LOGGER.debug("unregisterService: {}", serviceId);
        Service service = services.get(serviceId);
        if (service != null) {
            try {
                consulClient.agentServiceDeregister(serviceId);
                services.remove(serviceId);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void registerService(Service service) {
        LOGGER.debug("registerService: {}", service);
        service.setLastKnownStatus(ServiceStatus.DOWN);
        service.setServiceUUID(generateServiceUUID(service));
        services.put(service.getServiceId(), service);
        NewService.Check serviceCheck = new NewService.Check();
        serviceCheck.setHttp("http://" +
                managerConfigurationProperties.getExternalHost() + ":" +
                sideCarConfigurationProperties.getHttpPort() + "/v1/health/" + service.getServiceId());
        serviceCheck.setInterval("5s");
        serviceCheck.setTimeout("2s");
        //serviceCheck.setDeregisterCriticalServiceAfter("2m");
        NewService newService = new NewService();
        newService.setId(service.getServiceId());
        newService.setAddress(service.getIp());
        newService.setPort(service.getSideCarGrpcPort());
        newService.setName(getServiceName(service));
        newService.setTags(Arrays.asList("version=" + service.getServiceVersion()));
        newService.setCheck(serviceCheck);
        try {
            consulClient.agentServiceRegister(newService);
            LOGGER.debug("registered in Consul: {}", service);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public Service getService(String serviceId) {
        return services.get(serviceId);
    }
}
