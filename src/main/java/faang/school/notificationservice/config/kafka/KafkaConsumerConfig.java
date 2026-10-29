package faang.school.notificationservice.config.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler;
import org.springframework.kafka.listener.CommonErrorHandler;

@Configuration
@ConditionalOnProperty(
        prefix = "notifications",
        name = "transport",
        havingValue = "kafka"
)
public class KafkaConsumerConfig {

    @Bean
    public CommonErrorHandler postCreatedKafkaErrorHandler() {
        return new CommonContainerStoppingErrorHandler();
    }
}