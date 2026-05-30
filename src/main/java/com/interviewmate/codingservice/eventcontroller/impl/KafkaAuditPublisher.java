package com.interviewmate.codingservice.eventcontroller.impl;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.interviewmate.codingservice.dto.events.SecurityAuditEvent;
import com.interviewmate.codingservice.eventcontroller.AuditEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaAuditPublisher implements AuditEventPublisher {

    private final KafkaSender<String, SecurityAuditEvent> kafkaSender;

    @Value("${audit.kafka.topic:security-audit-events}")
    private String topic;

    @Override
    public void publish(SecurityAuditEvent event) {

        ProducerRecord<String, SecurityAuditEvent> producerRecord = new ProducerRecord<>(topic, event.getUserId(), event);
        SenderRecord<String, SecurityAuditEvent, String> senderRecord = SenderRecord.create(producerRecord, event.getEventId());

        kafkaSender.send(Mono.just(senderRecord))
            .next()
            .doOnSuccess(senderResult -> {
                if (senderResult != null && senderResult.recordMetadata() != null) {
                    log.debug(
                        "Audit published | eventId={} type={} offset={}",
                        event.getEventId(),
                        event.getEventType(),
                        senderResult.recordMetadata().offset()
                    );
                }
            })
            .doOnError(ex ->
                log.error(
                    "Audit publish failed | eventId={} type={} | {}",
                    event.getEventId(),
                    event.getEventType(),
                    ex.getMessage()
                )
            )
            .onErrorResume(ex -> Mono.empty())
            .subscribe();
    }
}