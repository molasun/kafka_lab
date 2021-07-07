/*
 * Copyright 2017-2020 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.redhat.kafkademo.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Tracer;
import io.opentracing.contrib.kafka.spring.TracingConsumerFactory;
import io.opentracing.contrib.kafka.spring.TracingKafkaAspect;
import io.opentracing.contrib.kafka.spring.TracingProducerFactory;

@Configuration
//@EnableKafka
public class KafkaConfiguration {

	@Value("${jaeger.tracer.host}")
	private String jaegerHost;
	@Value("${jaeger.tracer.port}")
	private Integer jaegerPort;
	@Value("${spring.application.name}")
	private String applicationName;

	@Bean
	public Tracer tracer() {
		return io.jaegertracing.Configuration.fromEnv(applicationName)
				.withSampler(
						io.jaegertracing.Configuration.SamplerConfiguration.fromEnv().withType(ConstSampler.TYPE).withParam(1))
				.withReporter(io.jaegertracing.Configuration.ReporterConfiguration.fromEnv().withLogSpans(true)
						.withFlushInterval(1000).withMaxQueueSize(10000)
						.withSender(io.jaegertracing.Configuration.SenderConfiguration.fromEnv()
								.withAgentHost(jaegerHost).withAgentPort(jaegerPort)))
				.getTracer();
	}

	// Decorate ConsumerFactory with TracingConsumerFactory
	@Bean
	public ConsumerFactory<String, String> consumerFactory() {
		Map<String, Object> consumerProps = new HashMap<String, Object>();
		consumerProps.put("bootstrap.servers", "my-cluster-kafka-bootstrap:9093");
		consumerProps.put("group.id", "sample-consumer");
		consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		//consumerProps.put("ssl.keystore.location", "src/main/resources/truststore.jks");
		//consumerProps.put("ssl.keystore.password", "P@ssw0rd");
		consumerProps.put("security.protocol", "SASL_PLAINTEXT");
		consumerProps.put("sasl.mechanism", "SCRAM-SHA-512");
		consumerProps.put("sasl.jaas.config", "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"my-user\" password=\"J8gJhFe3cOa3\";");
		return new TracingConsumerFactory<>(new DefaultKafkaConsumerFactory<>(consumerProps), tracer());
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
		ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory());
		return factory;
	}

	// Decorate ProducerFactory with TracingProducerFactory
	@Bean
	public ProducerFactory<String, String> producerFactory() {
		Map<String, Object> producerProps = new HashMap<String, Object>();
		producerProps.put("bootstrap.servers", "my-cluster-kafka-bootstrap:9093");
		producerProps.put("acks", "all");
		producerProps.put("retries", 0);
		producerProps.put("batch.size", 16384);
		producerProps.put("linger.ms", 1);
		producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		//producerProps.put("ssl.keystore.location", "src/main/resources/truststore.jks");
		//producerProps.put("ssl.keystore.password", "P@ssw0rd");
		producerProps.put("security.protocol", "SASL_PLAINTEXT");
		producerProps.put("sasl.mechanism", "SCRAM-SHA-512");
		producerProps.put("sasl.jaas.config", "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"my-user\" password=\"J8gJhFe3cOa3\";");
		return new TracingProducerFactory<>(new DefaultKafkaProducerFactory<>(producerProps), tracer());
	}

	// Use decorated ProducerFactory in KafkaTemplate
	@Bean
	public KafkaTemplate<String, String> kafkaTemplate() {
		return new KafkaTemplate<>(producerFactory());
	}

	// Use an aspect to decorate @KafkaListeners
	@Bean
	public TracingKafkaAspect tracingKafkaAspect() {
		return new TracingKafkaAspect(tracer());
	}
}
