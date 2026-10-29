package faang.school.notificationservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.notificationservice.client.UserServiceClient;
import faang.school.notificationservice.dto.UserDto;
import faang.school.notificationservice.dto.event.PostCreatedEvent;
import faang.school.notificationservice.messaging.MessageBuilder;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostCreatedEventHandler {

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final UserServiceClient userServiceClient;
    private final MessageBuilder<PostCreatedEvent> messageBuilder;
    private final List<NotificationService> notificationServices;

    public void handle(String body) {
        PostCreatedEvent event = deserializeEvent(body);
        validateEvent(event);

        log.info("Processing PostCreatedEvent: {}", event);

        UserDto author = userServiceClient.getUser(event.authorId());
        String notificationText = messageBuilder.buildMessage(
                event,
                Locale.forLanguageTag("ru")
        );

        sendNotification(author, notificationText);
        log.info("Notification sent for postId={}", event.postId());
    }

    private void sendNotification(UserDto user, String notificationText) {
        UserDto.PreferredContact contact = user.getPreference();

        if (contact == null) {
            throw new IllegalStateException(
                    "Notification preference is missing for userId=" + user.getId()
            );
        }

        NotificationService service = notificationServices.stream()
                .filter(notificationService ->
                        notificationService.getPreferredContact() == contact)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No notification service for preference=" + contact
                ));

        service.send(user, notificationText);
    }

    private PostCreatedEvent deserializeEvent(String body) {
        try {
            return objectMapper.readValue(body, PostCreatedEvent.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "Failed to deserialize PostCreatedEvent",
                    exception
            );
        }
    }

    private void validateEvent(PostCreatedEvent event) {
        Objects.requireNonNull(event, "The event can't be null");
        Set<ConstraintViolation<PostCreatedEvent>> violations =
                validator.validate(event);

        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}