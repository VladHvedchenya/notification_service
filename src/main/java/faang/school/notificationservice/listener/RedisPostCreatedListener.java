package faang.school.notificationservice.listener;

import faang.school.notificationservice.service.PostCreatedEventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "notifications",
        name = "transport",
        havingValue = "redis",
        matchIfMissing = true
)
public class RedisPostCreatedListener implements MessageListener {

    private final PostCreatedEventHandler handler;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        handler.handle(body);
    }
}
