package faang.school.notificationservice.listener;

import faang.school.notificationservice.config.kafka.KafkaConsumerConfig;
import faang.school.notificationservice.service.PostCreatedEventHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler;
import org.springframework.kafka.listener.CommonErrorHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PostCreatedListenerConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfig.class)
            .withPropertyValues(
                    "notifications.kafka.topic=post_topic",
                    "spring.kafka.consumer.group-id=notification-service-post-created-v1"
            );

    @Test
    void shouldSelectOnlyRedisByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(RedisPostCreatedListener.class);
            assertThat(context).doesNotHaveBean(KafkaPostCreatedListener.class);
            assertThat(context).doesNotHaveBean(CommonErrorHandler.class);
        });
    }

    @Test
    void shouldSelectOnlyRedisWhenConfigured() {
        contextRunner.withPropertyValues("notifications.transport=redis").run(context -> {
            assertThat(context).hasSingleBean(RedisPostCreatedListener.class);
            assertThat(context).doesNotHaveBean(KafkaPostCreatedListener.class);
            assertThat(context).doesNotHaveBean(CommonErrorHandler.class);
        });
    }

    @Test
    void shouldSelectOnlyKafkaWithStoppingErrorHandlerWhenConfigured() {
        contextRunner.withPropertyValues("notifications.transport=kafka").run(context -> {
            assertThat(context).hasSingleBean(KafkaPostCreatedListener.class);
            assertThat(context).doesNotHaveBean(RedisPostCreatedListener.class);
            assertThat(context).hasSingleBean(CommonErrorHandler.class);
            assertThat(context.getBean(CommonErrorHandler.class))
                    .isInstanceOf(CommonContainerStoppingErrorHandler.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({RedisPostCreatedListener.class, KafkaPostCreatedListener.class, KafkaConsumerConfig.class})
    static class TestConfig {

        @Bean
        PostCreatedEventHandler handler() {
            return mock(PostCreatedEventHandler.class);
        }
    }
}
