#!/bin/sh

cat <<EOF > /hydrosphere/configs/envoy.json
{
  "listeners": [
EOF

if [ "$USE_APP_HTTP" == "true" ]; then
cat <<EOF >> /hydrosphere/configs/envoy.json
    {
      "address": "tcp://0.0.0.0:$ENVOY_HTTP_PORT",
      "filters": [
        {
          "type": "read",
          "name": "http_connection_manager",
          "config": {
            "codec_type": "http1",
            "idle_timeout_s": 840,
            "stat_prefix": "egress_http1",
            "use_remote_address": true,
            "server_name":"$SERVICE_TYPE-$SERVICE_NAME-$SERVICE_VERSION",
            "rds":{
              "cluster": "global-cluster-manager",
              "route_config_name": "http",
              "refresh_delay_ms": 5000
            },
            "filters": [
              {
                "type": "decoder",
                "name": "router",
                "config": {}
              }
            ]
          }
        }
      ]
    },
EOF
fi

cat <<EOF >> /hydrosphere/configs/envoy.json
    {
      "address": "tcp://0.0.0.0:$ENVOY_GRPC_PORT",
      "filters": [
        {
          "type": "read",
          "name": "http_connection_manager",
          "config": {
            "codec_type": "http2",
            "idle_timeout_s": 840,
            "stat_prefix": "egress_http2",
            "use_remote_address": true,
            "server_name":"$SERVICE_TYPE-$SERVICE_NAME-$SERVICE_VERSION",
            "rds":{
              "cluster": "global-cluster-manager",
              "route_config_name": "grpc",
              "refresh_delay_ms": 5000
            },
            "filters": [
              {
                "type": "decoder",
                "name": "router",
                "config": {}
              }
            ]
          }
        }
      ]
    }
  ],
  "admin": {
    "access_log_path": "/var/log/envoy/admin_access.log",
    "address": "tcp://0.0.0.0:$ENVOY_ADMIN_PORT"
  },
  "cluster_manager": {
    "clusters":[
      {
        "name": "global-cluster-manager",
        "connect_timeout_ms": 250,
        "type": "strict_dns",
        "lb_type": "round_robin",
        "hosts": [{"url": "tcp://$MANAGER_HOST:$MANAGER_PORT"}]
      }
    ],
    "sds": {
      "cluster": {
        "name": "sds",
        "connect_timeout_ms": 250,
        "type": "strict_dns",
        "lb_type": "round_robin",
        "hosts": [{"url": "tcp://$MANAGER_HOST:$MANAGER_PORT"}]
      },
      "refresh_delay_ms": 5000
    },
    "cds": {
      "cluster": {
        "name": "cds",
        "connect_timeout_ms": 250,
        "type": "strict_dns",
        "lb_type": "round_robin",
        "hosts": [{"url": "tcp://$MANAGER_HOST:$MANAGER_PORT"}]
      },
      "refresh_delay_ms": 5000
    }
  }
}
EOF

echo "file generated"