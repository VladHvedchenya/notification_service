package faang.school.notificationservice.config.redis;

import faang.school.notificationservice.listener.RedisPostCreatedListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@ConditionalOnProperty(
        prefix = "notifications",
        name = "transport",
        havingValue = "redis",
        matchIfMissing = true
)
public class RedisConfig {
    @Bean
    public ChannelTopic postCreatedTopic(@Value("${notifications.channels.post-created}") String channelName){
        return new ChannelTopic(channelName);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisPostCreatedListener redisPostCreatedListener,
            @Qualifier("postCreatedTopic")
            ChannelTopic channelTopic){
        RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
        redisMessageListenerContainer.setConnectionFactory(connectionFactory);
        redisMessageListenerContainer.addMessageListener(redisPostCreatedListener, channelTopic);
        return redisMessageListenerContainer;
    }
}
