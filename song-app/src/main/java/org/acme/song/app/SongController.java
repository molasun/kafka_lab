package org.acme.song.app;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import lombok.SneakyThrows;

@RestController
@RequestMapping("/forwardToKafka")
public class SongController {

    @Value(value = "${kafka.topic}")
    private String kafkaTopic;

    @Autowired
    private KafkaTemplate<Integer, String> kafkaTemplate;
    // private ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping(consumes = "application/json")
    @ResponseBody
    public DeferredResult<ResponseEntity<?>> forwardToKafka(@RequestBody String jsonInput)
            throws JsonProcessingException {

        final DeferredResult<ResponseEntity<?>> response = new DeferredResult<>();

        System.out.println("jsonInput:" + jsonInput + "v4");

        final ListenableFuture<SendResult<Integer, String>> future = kafkaTemplate.send(kafkaTopic, jsonInput);

        future.addCallback(new ListenableFutureCallback<SendResult<Integer, String>>() {

            @SneakyThrows
            @Override
            public void onSuccess(SendResult<Integer, String> result) {

                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(result).append(" offset=[").append(result.getRecordMetadata().offset())
                        .append("]");
                System.out.println(stringBuilder.toString());

                response.setResult(new ResponseEntity<>(stringBuilder.toString(), HttpStatus.OK));

            }

            @Override
            public void onFailure(Throwable ex) {
                System.out.println("Unable to send message=[" + jsonInput + "] due to : " + ex.getMessage());
                response.setErrorResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage()));
            }
        });

        return response;
    }

}