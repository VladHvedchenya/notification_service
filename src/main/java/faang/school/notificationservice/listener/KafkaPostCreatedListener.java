package faang.school.notificationservice.listener;

import faang.school.notificationservice.service.PostCreatedEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        prefix = "notifications",
        name = "transport",
        havingValue = "kafka"
)
public class KafkaPostCreatedListener {

    private final PostCreatedEventHandler handler;

    @KafkaListener(
            topics = "${notifications.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        log.info(
                "Received Kafka message: topic={}, partition={}, offset={}, key={}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key()
        );

        handler.handle(record.value());
    }
}