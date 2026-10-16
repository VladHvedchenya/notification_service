package faang.school.notificationservice.messaging;

import faang.school.notificationservice.dto.event.PostCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class PostCreatedMessageBuilder implements MessageBuilder<PostCreatedEvent>{
    private final MessageSource messageSource;

    @Override
    public Class<?> getInstance() {
        return PostCreatedEvent.class;
    }

    @Override
    public String buildMessage(PostCreatedEvent event, Locale locale) {
        return messageSource.getMessage(
                "post.created",
                new Object[] {event.postId().toString()},
                locale);
    }
}
