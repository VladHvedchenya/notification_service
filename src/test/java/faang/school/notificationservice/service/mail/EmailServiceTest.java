package faang.school.notificationservice.service.mail;

import faang.school.notificationservice.dto.UserDto;
import faang.school.notificationservice.metrics.EmailNotificationMetrics;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {
    @Mock
    private JavaMailSender javaMailSender;

    private MockClock clock;
    private SimpleMeterRegistry meterRegistry;
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        MailProperties mailProperties = new MailProperties();
        mailProperties.setUsername("sender@example.com");
        clock = new MockClock();
        meterRegistry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock);
        EmailNotificationMetrics metrics = new EmailNotificationMetrics(meterRegistry);
        emailService = new EmailService(javaMailSender, mailProperties, metrics);
    }

    @AfterEach
    void tearDown() {
        meterRegistry.close();
    }

    @Test
    void shouldSendBuiltMessageToUserEmail() {
        UserDto user = new UserDto();
        user.setEmail("recipient@example.com");

        emailService.send(user, "Post #12345 created");

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(mailCaptor.capture());
        SimpleMailMessage mail = mailCaptor.getValue();
        assertThat(mail.getFrom()).isEqualTo("sender@example.com");
        assertThat(mail.getTo()).containsExactly("recipient@example.com");
        assertThat(mail.getSubject()).isEqualTo("Уведомление Dev Social Network");
        assertThat(mail.getText()).isEqualTo("Post #12345 created");
        assertThat(meterRegistry.get("notifications.email.send")
                .tag("outcome", "success").timer().count()).isEqualTo(1);
        assertThat(meterRegistry.get("notifications.email.send")
                .tag("outcome", "error").timer().count()).isZero();
    }

    @Test
    void shouldRecordSuccessfulSendDuration() {
        UserDto user = new UserDto();
        user.setEmail("recipient@example.com");
        doAnswer(invocation -> {
            clock.add(Duration.ofMillis(125));
            return null;
        }).when(javaMailSender).send(any(SimpleMailMessage.class));

        emailService.send(user, "Post created");

        assertThat(meterRegistry.get("notifications.email.send")
                .tag("outcome", "success").timer().count()).isEqualTo(1);
        assertThat(meterRegistry.get("notifications.email.send")
                .tag("outcome", "success").timer().totalTime(TimeUnit.MILLISECONDS))
                .isEqualTo(125.0);
        assertThat(meterRegistry.get("notifications.email.send")
                .tag("outcome", "error").timer().count()).isZero();
    }

    @Test
    void shouldDeclareEmailContact() {
        assertThat(emailService.getPreferredContact()).isEqualTo(UserDto.PreferredContact.EMAIL);
        assertThat(meterRegistry.get("notifications.email.send")
                .tag("outcome", "success").timer().count()).isZero();
        assertThat(meterRegistry.get("notifications.email.send")
                .tag("outcome", "error").timer().count()).isZero();
    }

    @Test
    void shouldNotHideSmtpFailure() {
        MailSendException failure = new MailSendException("SMTP unavailable");
        doAnswer(invocation -> {
            clock.add(Duration.ofMillis(75));
            throw failure;
        }).when(javaMailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailService.send(new UserDto(), "Post created"))
                .isSameAs(failure);
        assertThat(meterRegistry.get("notifications.email.send")
                .tag("outcome", "error").timer().count()).isEqualTo(1);
        assertThat(meterRegistry.get("notifications.email.send")
                .tag("outcome", "error").timer().totalTime(TimeUnit.MILLISECONDS))
                .isEqualTo(75.0);
        assertThat(meterRegistry.get("notifications.email.send")
                .tag("outcome", "success").timer().count()).isZero();
    }
}
