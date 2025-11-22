package com.planify.eventmanager.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaConsumer {
    
    @KafkaListener(topics = "event-created", groupId = "${spring.application.name}")
    public void consumeEventCreated(String message) {
        log.info("Consumed message from event-created: {}", message);
    }
    
    @KafkaListener(topics = "event-updated", groupId = "${spring.application.name}")
    public void consumeEventUpdated(String message) {
        log.info("Consumed message from event-updated: {}", message);
    }
    
    @KafkaListener(topics = "event-deleted", groupId = "${spring.application.name}")
    public void consumeEventDeleted(String message) {
        log.info("Consumed message from event-deleted: {}", message);
    }
}