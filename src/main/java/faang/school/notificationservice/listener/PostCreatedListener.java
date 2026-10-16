package faang.school.notificationservice.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.notificationservice.client.UserServiceClient;
import faang.school.notificationservice.dto.UserDto;
import faang.school.notificationservice.dto.event.PostCreatedEvent;
import faang.school.notificationservice.messaging.MessageBuilder;
import faang.school.notificationservice.service.NotificationService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Component
@Slf4j
@AllArgsConstructor
public class PostCreatedListener implements MessageListener {
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final UserServiceClient userServiceClient;
    private final MessageBuilder<PostCreatedEvent> messageBuilder;
    private final List<NotificationService> notificationServices;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        PostCreatedEvent postCreatedEvent = deserializeEvent(body);
        validateEvent(postCreatedEvent);
        log.info("Received Redis message: channel={}, body={}", channel, body);
        log.info("Received PostCreatedEvent: {}", postCreatedEvent);
        UserDto author = userServiceClient.getUser(postCreatedEvent.authorId());
        log.info("Loaded post author: id={}, username={}, preference={}",
                author.getId(), author.getUsername(), author.getPreference());
        String notificationText = messageBuilder.buildMessage(postCreatedEvent, Locale.forLanguageTag("ru"));
        log.info("Message: {}", notificationText);
        sendNotification(author, notificationText);
    }

    private void sendNotification(UserDto user, String notificationText){
        UserDto.PreferredContact contact = user.getPreference();

        if (contact == null){
            throw new IllegalStateException(
                    "Notification preference is missing for userId=" + user.getId());
        }

        NotificationService service = notificationServices
                .stream()
                .filter(notificationService -> notificationService.getPreferredContact() == contact)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No notification service for preference=" + contact));
        service.send(user, notificationText);
    }

    private PostCreatedEvent deserializeEvent(String body){
        try{
            return objectMapper.readValue(body, PostCreatedEvent.class);
        }
        catch (JsonProcessingException e){
            throw new IllegalArgumentException("Failed to deserialize PostCreatedEvent", e);
        }
    }

    private void validateEvent(PostCreatedEvent event){
        Objects.requireNonNull(event, "The event can't be null");
        Set<ConstraintViolation<PostCreatedEvent>> violations = validator.validate(event);

        if(!violations.isEmpty()){
            throw new ConstraintViolationException(violations);
        }
    }
}
