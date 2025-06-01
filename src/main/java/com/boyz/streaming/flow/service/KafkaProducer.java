package com.boyz.streaming.flow.service;

import com.boyz.streaming.flow.dto.SendRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public void send(SendRequestDTO req) {
        kafkaTemplate.send(req.getTopic(), req.getMessage());
    }
}
