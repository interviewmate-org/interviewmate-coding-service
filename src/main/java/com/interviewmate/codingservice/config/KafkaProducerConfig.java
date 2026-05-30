package com.interviewmate.codingservice.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.interviewmate.codingservice.dto.events.SecurityAuditEvent;

import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public KafkaSender<String, SecurityAuditEvent> auditKafkaSender(ObjectMapper objectMapper) {

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,        bootstrapServers);

        // Reliability
        props.put(ProducerConfig.ACKS_CONFIG,             "1");
        props.put(ProducerConfig.RETRIES_CONFIG,          3);
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 200);

        // Throughput
        props.put(ProducerConfig.LINGER_MS_CONFIG,    5);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG,   16384);

        // Configure Serializers using Jackson 3 ObjectMapper
        StringSerializer keySerializer = new StringSerializer();
        Serializer<SecurityAuditEvent> valueSerializer = new Serializer<SecurityAuditEvent>() {
            @Override
            public byte[] serialize(String topic, SecurityAuditEvent data) {
                if (data == null) {
                    return null;
                }
                try {
                    return objectMapper.writeValueAsBytes(data);
                } catch (Exception e) {
                    throw new RuntimeException("Error serializing SecurityAuditEvent", e);
                }
            }
        };

        SenderOptions<String, SecurityAuditEvent> senderOptions = SenderOptions.<String, SecurityAuditEvent>create(props)
                .withKeySerializer(keySerializer)
                .withValueSerializer(valueSerializer);

        return KafkaSender.create(senderOptions);
    }
}