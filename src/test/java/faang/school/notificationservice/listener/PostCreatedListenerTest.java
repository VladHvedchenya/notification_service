package faang.school.notificationservice.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.notificationservice.client.UserServiceClient;
import faang.school.notificationservice.dto.UserDto;
import faang.school.notificationservice.dto.event.PostCreatedEvent;
import faang.school.notificationservice.messaging.MessageBuilder;
import faang.school.notificationservice.service.NotificationService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class PostCreatedListenerTest {
    private static ValidatorFactory validatorFactory;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private MessageBuilder<PostCreatedEvent> messageBuilder;

    @Mock
    private NotificationService emailService;

    @Mock
    private NotificationService smsService;

    private PostCreatedListener listener;

    @BeforeAll
    static void createValidatorFactory() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @BeforeEach
    void setUp() {
        listener = new PostCreatedListener(
                new ObjectMapper(),
                validatorFactory.getValidator(),
                userServiceClient,
                messageBuilder,
                List.of(emailService, smsService));
    }

    @Test
    void shouldLoadAuthorBuildTextAndSelectPreferredSender() {
        UserDto user = user(UserDto.PreferredContact.PHONE);
        when(userServiceClient.getUser(1L)).thenReturn(user);
        when(messageBuilder.buildMessage(new PostCreatedEvent(12345L, 1L, null), Locale.forLanguageTag("ru")))
                .thenReturn("Post created");
        when(emailService.getPreferredContact()).thenReturn(UserDto.PreferredContact.EMAIL);
        when(smsService.getPreferredContact()).thenReturn(UserDto.PreferredContact.PHONE);

        listener.onMessage(message("{\"postId\":12345,\"authorId\":1,\"projectId\":null}"), null);

        verify(userServiceClient).getUser(1L);
        verify(smsService).send(user, "Post created");
        verify(emailService, never()).send(any(), anyString());
    }

    @Test
    void shouldRejectMalformedJsonBeforeLoadingUser() {
        assertThatThrownBy(() -> listener.onMessage(message("{broken"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deserialize");
        verifyNoInteractions(userServiceClient, messageBuilder, emailService, smsService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"postId\":12345}", "{\"authorId\":1}"})
    void shouldValidateRequiredEventFields(String body) {
        assertThatThrownBy(() -> listener.onMessage(message(body), null))
                .isInstanceOf(ConstraintViolationException.class);
        verifyNoInteractions(userServiceClient, messageBuilder, emailService, smsService);
    }

    @Test
    void shouldRejectJsonNull() {
        assertThatThrownBy(() -> listener.onMessage(message("null"), null))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(userServiceClient, messageBuilder, emailService, smsService);
    }

    @Test
    void shouldRejectMissingPreferenceWithUserId() {
        prepareUser(null);

        assertThatThrownBy(() -> listener.onMessage(validMessage(), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("userId=1");
        verifyNoInteractions(emailService, smsService);
    }

    @Test
    void shouldExplainUnsupportedPreference() {
        prepareUser(UserDto.PreferredContact.TELEGRAM);

        assertThatThrownBy(() -> listener.onMessage(validMessage(), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TELEGRAM");
    }

    @Test
    void shouldPropagateUserServiceFailureWithoutSending() {
        RuntimeException failure = new RuntimeException("User service unavailable");
        when(userServiceClient.getUser(1L)).thenThrow(failure);

        assertThatThrownBy(() -> listener.onMessage(validMessage(), null)).isSameAs(failure);
        verifyNoInteractions(messageBuilder, emailService, smsService);
    }

    private void prepareUser(UserDto.PreferredContact preference) {
        when(userServiceClient.getUser(1L)).thenReturn(user(preference));
    }

    private UserDto user(UserDto.PreferredContact preference) {
        UserDto user = new UserDto();
        user.setId(1L);
        user.setUsername("Author");
        user.setPreference(preference);
        return user;
    }

    private Message validMessage() {
        return message("{\"postId\":12345,\"authorId\":1}");
    }

    private Message message(String body) {
        return new DefaultMessage("post.created".getBytes(StandardCharsets.UTF_8), body.getBytes(StandardCharsets.UTF_8));
    }
}
