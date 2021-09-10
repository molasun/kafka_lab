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

