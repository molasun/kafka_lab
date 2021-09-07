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
  * Create with yaml file, see [kafka-my-cluster](https://github.com/snowfish424/kafka_lab/edit/main/install/kafka-my-cluster.yaml) , make sure condition is ready
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
  * Create with yaml file, see [kafkabridge-rest](https://github.com/snowfish424/kafka_lab/blob/main/install/kafkabridge-rest.yaml) , make sure condition is ready
    ![image](https://user-images.githubusercontent.com/58408898/132298117-a59952e1-8bbc-4bdd-81c8-de4edf934ebf.png)
  * Check cluster status, a kafka-bridge pod should be created
    ![image](https://user-images.githubusercontent.com/58408898/132298800-264466d3-2273-470d-9afe-fb028fea4097.png)

## Create and set kafka topic

* Use intalled operator to create Kafka topic
  * Choose Kafka topic resource and create, there are 3 default topics
    * consumer-offsets
    * strimzi-store-topic
    * strimzi-topic-operator-kstreams-topic-store-changelog
    ![image](https://user-images.githubusercontent.com/58408898/132300235-87c78d8b-a738-4dfa-9e25-ee4b813d7eb1.png)
  * 
