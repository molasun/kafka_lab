package org.acme.song.app;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Consumer {

    @KafkaListener(topics = "${kafka.topic}")
    public void processMessage(Song message,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) List<Integer> partitions,
            @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) List<String> key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) List<String> topics,
            @Header(KafkaHeaders.OFFSET) List<Long> offsets) {
        System.out.printf(this.getClass().getSimpleName() + " receive=>  %s-%d[%d] \"%s\"\n", topics.get(0),
                partitions.get(0), offsets.get(0), message);
    }

}
