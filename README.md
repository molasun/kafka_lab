# kafka_lab

# Install

## Install AMQ streams cluster using operator

* Search operator hub and install
  * Choose AMQ Streams operator (based on v1.8)
    ![image](https://user-images.githubusercontent.com/58408898/132277757-daa0d56d-8054-4898-8027-727eeaf89655.png)
  * Choose prefer channel and the namespace
    ![image](https://user-images.githubusercontent.com/58408898/132279495-6517bb00-1e3a-494c-9175-669e9391b131.png)
* Use installed operator to install Kafka cluster
  * Choose Kafka resource and create
    ![image](https://user-images.githubusercontent.com/58408898/132279765-2e403ba4-a9b8-4b45-91df-5299d9f66187.png)
  * Create with yaml file, see [kafka-my-cluster](https://github.com/snowfish424/kafka_lab/edit/main/install/kafka-my-cluster.yaml) , make sure the condition is ready
    ![image](https://user-images.githubusercontent.com/58408898/132281401-9f5fcb87-7b4d-4be5-bea5-d0b82aac9a72.png)
  * Check cluster status, a standard HA cluster should included
    * 1 cluster operator
    * 1 entity operator
    * 3 kafka brokers
    * 3 zookeepers
    * 1 kafka exporter
    ![image](https://user-images.githubusercontent.com/58408898/132294507-bf1e073e-fb31-421c-9c7c-9087474882b4.png)
* Use installed operator to install Kafka bridge
  * Choose Kafka bridge resource and create
    ![image](https://user-images.githubusercontent.com/58408898/132295310-0f9c87ab-7b07-4db0-9c52-30515bbac223.png) 
  * Create with yaml file, see [kafkabridge-rest](https://github.com/snowfish424/kafka_lab/blob/main/install/kafkabridge-rest.yaml) , make sure the condition is ready
    ![image](https://user-images.githubusercontent.com/58408898/132298117-a59952e1-8bbc-4bdd-81c8-de4edf934ebf.png)
  * Check cluster status, a kafka-bridge pod should be created
    ![image](https://user-images.githubusercontent.com/58408898/132298800-264466d3-2273-470d-9afe-fb028fea4097.png)
  * Expose service to route
    ```     
    oc expose svc http-bridge-bridge-service
    ```
  * Check the route status with curl command
    ```
    curl -v GET http://http-bridge-bridge-service-amqs-01.apps.cluster-5pdkv.5pdkv.sandbox1270.opentlc.com/healthy
    ```

## Create and set kafka topic

* Use installed operator to create Kafka topic
  * Choose Kafka topic resource and create, there are 3 default topics
    * consumer-offsets
    * strimzi-store-topic
    * strimzi-topic-operator-kstreams-topic-store-changelog
    ![image](https://user-images.githubusercontent.com/58408898/132300235-87c78d8b-a738-4dfa-9e25-ee4b813d7eb1.png)
  * Create with yaml file, make sure new topic condition is ready
    ![image](https://user-images.githubusercontent.com/58408898/132319195-1c48bc5f-b332-47fe-9467-fdec1b893940.png)
  * Test topic with http bridge
    * Produce messages via kafka bridge
    ```
    # Check available topics
    curl -v GET http://<http-bridge-route>/topics
    
    # Produce via http bridge
    curl -X POST \
      http://<http-bridge-route>/topics/my-topic \
      -H 'content-type: application/vnd.kafka.json.v2+json' \
      -d '{
        "records": [
            {
                "key": "01",
                "value": "20210910-16:00-1"
            },
            {
                "key": "02",
                "value": "20210910-16:00-2"
            }
        ]
    }'
    ``` 
    * Check message with conumser console
    ``` 
    oc run kafka-consumer -ti --image=registry.redhat.io/amq7/amq-streams-kafka-23:1.3.0 --rm=true --restart=Never -- /bin/bash -c "cat >/tmp/consumer.properties <<EOF
    security.protocol=SASL_PLAINTEXT
    sasl.mechanism=SCRAM-SHA-512
    sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required username="my-user" password="<password>;
    EOF
    bin/kafka-console-consumer.sh --bootstrap-server my-cluster-kafka-bootstrap:9093 --topic my-topic --from-beginning --consumer.config=/tmp/consumer.properties
    "    
    ``` 

* Use installed operator to create Kafka user
  * Choose Kafka user resource and create
    ![image](https://user-images.githubusercontent.com/58408898/132319684-f38a4ee5-5ca0-4667-a037-be7f732e994a.png)
  * Create with yaml file, see [kafkauser-my-user](https://github.com/snowfish424/kafka_lab/blob/main/install/kafkauser-my-user.yaml) , make sure the condition is ready
    ![image](https://user-images.githubusercontent.com/58408898/132320109-301f9cc6-9f1a-41ad-8cfc-3a1cccc14a9b.png)

# Write and test Kafka client

## Create certification

* Create Kafka cluster certification
  * If you need to access with external route, then below step is needed
  ```
  export CLUSTER_NAME=my-cluster
  
  # export cert from Kafka cluster secert
  oc get secret $CLUSTER_NAME-cluster-ca-cert -o jsonpath='{.data.ca\.crt}' | base64 --decode > ca.crt
  
  # export password from Kafka cluster secret
  oc get secret $CLUSTER_NAME-cluster-ca-cert -o jsonpath='{.data.ca\.password}' | base64 --decode > ca.password
  ```   
* Create and import Kafka cluster CA to truststore
  ``` 
  export CERT_FILE_PATH=ca.crt
  export CERT_PASSWORD_FILE_PATH=ca.password
  export KEYSTORE_LOCATION=~/kafka-demo/src/main/resources/truststore.jks
  export PASSWORD=`cat $CERT_PASSWORD_FILE_PATH`

  # import CA trust store
  keytool -importcert -alias strimzi-kafka-cert -file $CERT_FILE_PATH -keystore $KEYSTORE_LOCATION -keypass $PASSWORD
  ```   

* Create Kafka user password
  ``` 
  oc get secret my-user -o jsonpath='{.data.password}' | base64 --decode > user-scram.password
  ``` 
  
## Kafka client example

* Kafka client producer config
  ```java 
  @Bean
  public ProducerFactory<String, String> producerFactory() {
		        Map<String, Object> producerProps = new HashMap<String, Object>();
		        producerProps.put("bootstrap.servers", "my-cluster-kafka-bootstrap:9093");
		        producerProps.put("acks", "all");
		        producerProps.put("retries", 2);
		        producerProps.put("max.block.ms", 5000);
		        producerProps.put("batch.size", 16384);
		        producerProps.put("linger.ms", 1);
		        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		        producerProps.put("security.protocol", "SASL_PLAINTEXT");
		        producerProps.put("sasl.mechanism", "SCRAM-SHA-512");
		        producerProps.put("sasl.jaas.config", "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"my-user\" password=\"<password>\";");
		        return new TracingProducerFactory<>(new DefaultKafkaProducerFactory<>(producerProps), tracer());
  }
  ```   
* Kafka client consumer config
  ```java
  @Bean
  public ConsumerFactory<String, String> consumerFactory() {
		        Map<String, Object> consumerProps = new HashMap<String, Object>();
		        consumerProps.put("bootstrap.servers", "my-cluster-kafka-bootstrap:9093");
		        consumerProps.put("group.id", "sample-consumer");
		        consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		        consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		        consumerProps.put("security.protocol", "SASL_PLAINTEXT");
		        consumerProps.put("sasl.mechanism", "SCRAM-SHA-512");
		        consumerProps.put("sasl.jaas.config", "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"my-user\" password=\"<password>\";");
		        return new TracingConsumerFactory<>(new DefaultKafkaConsumerFactory<>(consumerProps), tracer());
  }
  ```

# Kafka Monitoring

## Create Metrics and Dashboard
* Set up user workload monitoring
  * Create kafka-metrics configmap in kafka namespace, see [kafka-metrics-configmap](https://github.com/snowfish424/kafka_lab/blob/main/monitor/grafana/kafka-metrics-configmap.yaml)
  * Create cluster-monitoring-config configmap in openshift-monitoring namespace (cluster admin role is needed), see [cluster-monitoring-config.yaml](https://github.com/snowfish424/kafka_lab/blob/main/monitor/grafana/cluster-monitoring-config.yaml)
  * Create user-workload-monitoring-config configmap in openshift-user-workload-monitoring namesapce (cluster admin role is needed), see [user-workload-monitoring-config.yaml](https://github.com/snowfish424/kafka_lab/blob/main/monitor/grafana/user-workload-monitoring-config.yaml)
  * Enable user workload in cluster-monitoring-config configmap (cluster admin role is needed)
    ```yaml
    kind: ConfigMap
    apiVersion: v1
    ...
    data:
      config.yaml: |
        enableUserWorkload: true
    ```
  * Check monitoring workload is running
    ```
    oc get pod -n openshift-user-workload-monitoring       
    ```
    You should see below pods running
    ```
    NAME                                   READY   STATUS    RESTARTS   AGE
    prometheus-operator-6f96b4b8f8-hls48   2/2     Running   0          10h
    prometheus-user-workload-0             5/5     Running   1          10h
    prometheus-user-workload-1             5/5     Running   1          10h
    thanos-ruler-user-workload-0           3/3     Running   0          10h
    thanos-ruler-user-workload-1           3/3     Running   0          10h
    ```
* Set up Prometheus rule
  * Apply strimzi-pod-monitor, see [strimzi-pod-monitor.yaml](https://github.com/snowfish424/kafka_lab/blob/main/monitor/prometheus/strimzi-pod-monitor.yaml)
    ```
    oc apply -f strimzi-pod-monitor.yaml -n <your-kafka-project>
    ```
    You should see below resources created
    ```
    podmonitor.monitoring.coreos.com/cluster-operator-metrics created
    podmonitor.monitoring.coreos.com/entity-operator-metrics created
    podmonitor.monitoring.coreos.com/bridge-metrics created
    podmonitor.monitoring.coreos.com/kafka-resources-metrics created
    ```
  * Apply Prometheus rule, see [prometheus-rules.yaml](https://github.com/snowfish424/kafka_lab/blob/main/monitor/prometheus/prometheus-rules.yaml)
    ```
    oc apply -f prometheus-rules.yaml -n <your-kafka-project>
    ```
    You should see below resource created
    ```
    prometheusrule.monitoring.coreos.com/prometheus-k8s-rules created
    ```
* Set up Grafana service account
  *  Create grafana-serviceaccount, see [grafana-serviceaccount.yaml](https://github.com/snowfish424/kafka_lab/blob/main/monitor/grafana/grafana-serviceaccount.yaml)
    ```
    oc apply -f grafana-serviceaccount.yaml -n <your-kafka-project>
    ```
    You should see below resource created
    ```
    serviceaccount/grafana-serviceaccount created
    ```
  *  Create grafana-cluster-monitoring-binding, see [grafana-cluster-monitoring-binding.yaml](https://github.com/snowfish424/kafka_lab/blob/main/monitor/grafana/grafana-cluster-monitoring-binding.yaml)
    ```
    oc apply -f grafana-cluster-monitoring-binding.yaml -n <your-kafka-project>
    ```
    You should see below resource created
    ```
    clusterrolebinding.rbac.authorization.k8s.io/grafana-cluster-monitoring-binding created
    ```
* Set up Grafana dashboard
  * Get grafana-serviceaccount token
    ```
    oc serviceaccounts get-token grafana-serviceaccount -n <your-kafka-project>
    ```
  * Create datasource.yaml and put the above token in secureJsonData.httpHeaderValue1 field
    ```yaml
    ...  
      secureJsonData:
        httpHeaderValue1: "Bearer <your token>"
    ```
  * Create grafana-config configmap with datasource.yaml
    ```
    oc create configmap grafana-config --from-file=datasource.yaml -n <your-kafka-project>
    ```
    You should see below resource created
    ```
    configmap/grafana-config created
    ```
  * Apply Grafana dashboard, see [grafana.yaml](https://github.com/snowfish424/kafka_lab/blob/main/monitor/grafana/grafana.yaml)
    ```
    oc apply -f grafana.yaml -n <your-kafka-project>
    ```
    You should see below resource created
    ```
    persistentvolumeclaim/grafana-data created
    deployment.apps/grafana created
    service/grafana created
    route.route.openshift.io/grafana created
    ```
* Add kafka and zookeeper metrics config in Kafka CR
    ```yaml
    spec:
    ...
      kafka:
        metricsConfig:
          type: jmxPrometheusExporter
          valueFrom:
            configMapKeyRef:
              key: kafka-metrics-config.yml
              name: kafka-metrics
    ...
      zookeeper:
        metricsConfig:
          type: jmxPrometheusExporter
          valueFrom:
            configMapKeyRef:
              key: zookeeper-metrics-config.yml
              name: kafka-metrics  
    ```

## Login to Grafana and create basic dashboard
* Set up Strimzi Grafana dashboard
  * Login to Grafano through route, use default username and password(Should be admin/admin, you should change to new secure password)
    ![image](https://user-images.githubusercontent.com/58408898/132934688-a88be152-191d-472e-8da2-51b0b0af5df3.png) 
  * Check Prometheus datasource
    ![image](https://user-images.githubusercontent.com/58408898/132934963-f8d7dbb1-6b10-4c32-be27-88485d8e01c7.png) 
    ![image](https://user-images.githubusercontent.com/58408898/132934972-6b84a0a8-96b8-4ed5-a1c6-4796d0f5db5c.png)
  * Import basic dashboard, see [dashboard json](https://github.com/snowfish424/kafka_lab/tree/main/monitor/grafana/dashboard)
    * strimzi-kafka
    * strimzi-zookeeper
    * strimzi-kafka-exporter
    * strimzi-operator
    * strimzi-crusie-control
    ![image](https://user-images.githubusercontent.com/58408898/132935056-176248a3-1d4c-4908-9380-14321beae207.png)
  * Check wtih Dashboard to see the data show appropriately
    * Strimzi Kafka:
      ![image](https://user-images.githubusercontent.com/58408898/132935850-0960c17a-103a-4206-921b-c03d5090cf72.png)
    * Strimzi Kafka exporter:
      ![image](https://user-images.githubusercontent.com/58408898/132935888-8718e0c1-f020-481a-8179-5afdefd6bfb3.png)
    * Strimzi Zookeeper:
      ![image](https://user-images.githubusercontent.com/58408898/132935908-6d9dae01-f4dc-4402-a65a-f20e88431e99.png)

# Kafka Mirror Maker 2.0

## Install target cluster and mm2
* Create a target Kafka cluster
  * Repeat Kafka install procedure
  * Use installed operator to install Kafka mirror maker 2.0. There are 3 install scheme
    * Install at the source cluster project
    * Install at the target cluster project
    * Install at it's own independent project
    The sample procedure shown below follow the 2nd scheme, create mm2 at the target cluster project
    ![image](https://user-images.githubusercontent.com/58408898/132937585-f491683b-5c98-42be-9037-c4b49c98d099.png)
  * Create mm2-credentials for access source cluster topic via 9093 port, see [secret-mm2-credentials](https://github.com/snowfish424/kafka_lab/blob/main/install/secret-mm2-credentials.yaml)
  * Create mm2 with yaml file, see [kafkamirrormaker2-my-mm2-cluster](https://github.com/snowfish424/kafka_lab/blob/main/install/kafkamirrormaker2-my-mm2-cluster.yaml)
  * Check mm2 topic status, a standard HA cluster should included

# Kafka Client Load Test

## Producer Load Test
* Use Kafka producer perf tool
  * Run kafka-producer-perf-test.sh with SCRAM
    ```
    oc run kafka-producer -ti --image=registry.redhat.io/amq7/amq-streams-kafka-23:1.3.0 --rm=true --restart=Never -- /bin/bash -c "cat >/tmp/producer.properties <<EOF 
    security.protocol=SASL_PLAINTEXT
    sasl.mechanism=SCRAM-SHA-512
    sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required username="my-user" password="<password>";
    EOF
    bin/kafka-producer-perf-test.sh --topic my-topic --num-records 1000 --record-size 1000 --throughput 50 --producer-props bootstrap.servers=my-cluster-kafka-bootstrap:9093 acks=all --producer.config=/tmp/producer.properties --print-metric
    "
    ```
    * total records sent
    * records/sec
    * avg latency
    * max latency
    ![image](https://user-images.githubusercontent.com/58408898/132939580-af4b1d81-ba4c-4c43-8537-982b83970c62.png)

## Consumer Load Test
* Use Kafka consumer perf tool 
  * Run kafka-consumer-perf-test.sh with SCRAM
    ```
    oc run kafka-consumer -ti --image=registry.redhat.io/amq7/amq-streams-kafka-23:1.3.0 --rm=true --restart=Never -- /bin/bash -c "cat >/tmp/consumer.properties <<EOF
    security.protocol=SASL_PLAINTEXT
    sasl.mechanism=SCRAM-SHA-512
    sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required username="my-user" password="kxXxFJSWVtrb";
    EOF
    bin/kafka-consumer-perf-test.sh --broker-list my-cluster-kafka-bootstrap:9093 --topic my-topic-load-test --messages 100 --threads 3 --group load-test --consumer.config=/tmp/consumer.properties
    "
    ```    
    * start.time
    * end.time
    * data.consumed.in.MB
    * MB.sec
    * data.consumed.in.nMsg
    * nMsg.sec
    * rebalance.time.ms
    * fetch.time.ms
    * fetch.MB.sec
    * fetch.nMsg.sec
