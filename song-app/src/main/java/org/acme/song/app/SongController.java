package org.acme.song.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

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

        System.out.println("jsonInput:" + jsonInput);

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

    // private void sendHttp() throws JSONException {
    //     // String Url =
    //     // "http://ctbc-kafka-route-amq-stream.apps.ocp4.pretest.intra.ctbcbank.com/topics/my-topic";
    //     String Url = "http://localhost:8084/topics/my-topic";
    //     RestTemplate restTemplate = new RestTemplate();
    //     // HttpHeaders headers = new HttpHeaders();
    //     // headers.setContentType(MediaType.APPLICATION_JSON);
    //     // JSONObject personJsonObject = new JSONObject();
    //     // personJsonObject.put("key", "key-1");
    //     // personJsonObject.put("value", "value-1");
    //     String result = restTemplate.getForObject(Url, String.class);
    //     System.out.print("result:" + result);
    }

}