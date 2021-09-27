package org.acme.song.app;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

//@SpringBootApplication
@RunWith(SpringRunner.class)
@SpringBootTest
class SongAppApplicationTests {

	@Autowired
	private KafkaTemplate<Integer, String> kafkaTemplate;

	@Value("${kafka.topic}")
	private String topic;

	@Autowired
	Consumer consumer;

	//@Disabled
	@Test
	void contextLoads()  {
		int i = 0;
		while (true) {
			kafkaTemplate.send(topic, "test message from producer");
			System.out.println("test message from producer :"+ i);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			i++;
		}
	}



}
