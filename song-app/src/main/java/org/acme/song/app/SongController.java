package org.acme.song.app;

import java.net.InetAddress;
import java.net.UnknownHostException;

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
@RequestMapping("/song")
public class SongController {

    @Value(value = "${kafka.topic}")
    private String kafkaTopic;
    private String ip;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @PostMapping(path = "/create", consumes = "application/json")
    @ResponseBody
    public DeferredResult<ResponseEntity<?>> createSong(@RequestBody Song song)
            throws JsonProcessingException {

        final DeferredResult<ResponseEntity<?>> response = new DeferredResult<>();

        System.out.println("jsonInput:" + song + "v5");

        try {
            ip = InetAddress.getLocalHost().toString();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        final ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(kafkaTopic, ip, song);

        future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {

            @SneakyThrows
            @Override
            public void onSuccess(SendResult<String, Object> result) {

                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(result).append(" offset=[")
                        .append(result.getRecordMetadata().offset())
                        .append("]");
                System.out.println(stringBuilder.toString());

                response.setResult(new ResponseEntity<>(stringBuilder.toString(), HttpStatus.OK));

            }

            @Override
            public void onFailure(Throwable ex) {
                System.out.println("Unable to send message=[" + song + "] due to : " + ex.getMessage());
                response.setErrorResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage()));
            }
        });

        return response;
    }

}