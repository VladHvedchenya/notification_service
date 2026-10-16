package faang.school.notificationservice.service.mail;

import faang.school.notificationservice.dto.UserDto;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {
    @Mock
    private JavaMailSender javaMailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        MailProperties mailProperties = new MailProperties();
        mailProperties.setUsername("sender@example.com");
        emailService = new EmailService(javaMailSender, mailProperties);
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
        assertThat(mail.getSubject()).isEqualTo("Уведомление CorporationX");
        assertThat(mail.getText()).isEqualTo("Post #12345 created");
    }

    @Test
    void shouldDeclareEmailContact() {
        assertThat(emailService.getPreferredContact()).isEqualTo(UserDto.PreferredContact.EMAIL);
    }

    @Test
    void shouldNotHideSmtpFailure() {
        MailSendException failure = new MailSendException("SMTP unavailable");
        doThrow(failure).when(javaMailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailService.send(new UserDto(), "Post created"))
                .isSameAs(failure);
    }
}
