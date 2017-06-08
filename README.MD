###Build
1. Build this repository
```
mvn clean install
```
2. Build runtime https://github.com/provectus/hydro-serving/tree/dev_envoy
```
./build.sh
```
3. Run managers
```
docker-compose up consul zipkin manager
```
http://localhost:8500/ui/#/dc1/services/manager-global-cluster-manager
http://localhost:9411/

4. Run repository and gateway
```
docker-compose up gateway repository
```

5. Run runtime
```
docker-compose up localml-spark mist-runtime-scikit model1 model2
```

6. Create pipelines

```
curl -H "Content-Type: application/json" -X POST -d '{"name":"endpoint1", "chain":["serving-model1"]}' http://localhost:8083/api/v1/pipelines
curl -H "Content-Type: application/json" -X POST -d '{"name":"endpoint2", "chain":["serving-model1","serving-model2"]}' http://localhost:8080/api/v1/pipelines

curl -H "Content-Type: application/json" -X POST -d '{"name":"endpointScikit","transportType":"http", "chain":["serving-scikit/svm"]}' http://localhost:8080/api/v1/pipelines

curl -H "Content-Type: application/json" -X POST -d '{"name":"endpointSpark","transportType":"http", "chain":["serving-localml-spark/dtreeregressor"]}' http://localhost:8080/api/v1/pipelines

curl -H "Content-Type: application/json" -X POST -d '{"name":"endpointBoth","transportType":"http", "chain":["serving-scikit/svm","serving-localml-spark/dtreeregressor"]}' http://localhost:8080/api/v1/pipelines
```


7. Check 
```
curl -H "Content-Type: application/json" -X POST -d '[{"test":"test"}]' http://localhost:8083/api/v1/serve/endpoint1

curl -H "Content-Type: application/json" -X POST -d '[{"test":"test"}]' http://localhost:8083/api/v1/serve/endpoint2

curl --request POST \
  --url http://localhost:8083/api/v1/serve/endpointScikit \
  --header 'content-type: application/json' \
  --data '[{"sepal length (cm)":5.0,"sepal width (cm)":3.0,"petal length (cm)":1.6,"petal width (cm)":0.2,"features":[1,2,3,4,5],"color":12,"indexedFeatures":[1,2,3,1,0],"prediction":1},{"sepal length (cm)":5.9,"sepal width (cm)":3.0,"petal length (cm)":5.1,"petal width (cm)":1.8,"features":[8,0,5,1,7],"color":1,"indexedFeatures":[8,0,5,0,1],"prediction":0}]'
  
  
curl --request POST \
  --url http://localhost:8083/api/v1/serve/endpointSpark \
  --header 'content-type: application/json' \
  --data '[{"features": [1.0, 2.0, 3.0, 4.0, 5.0],"color": 12},{"features": [8.0, 0.0, 5.0, 1.0, 7.0],"color": 1}]'
  
curl --request POST \
  --url http://localhost:8083/api/v1/serve/endpointBoth \
  --header 'content-type: application/json' \
--data '[{"sepal length (cm)":5.0,"sepal width (cm)":3.0,"petal length (cm)":1.6,"petal width (cm)":0.2,"features":[1,2,3,4,5]},{"sepal length (cm)":5.9,"sepal width (cm)":3.0,"petal length (cm)":5.1,"petal width (cm)":1.8,"features":[8,0,5,1,7]}]'
```