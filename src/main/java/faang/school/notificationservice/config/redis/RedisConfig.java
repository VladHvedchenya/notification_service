package faang.school.notificationservice.config.redis;

import faang.school.notificationservice.listener.PostCreatedListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisConfig {
    @Bean
    public ChannelTopic postCreatedTopic(@Value("${notifications.channels.post-created}") String channelName){
        return new ChannelTopic(channelName);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            PostCreatedListener postCreatedListener,
            @Qualifier("postCreatedTopic")
            ChannelTopic channelTopic){
        RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
        redisMessageListenerContainer.setConnectionFactory(connectionFactory);
        redisMessageListenerContainer.addMessageListener(postCreatedListener, channelTopic);
        return redisMessageListenerContainer;
    }
}
