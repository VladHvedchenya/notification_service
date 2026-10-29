package faang.school.notificationservice.service.mail;

import faang.school.notificationservice.dto.UserDto;
import faang.school.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import faang.school.notificationservice.metrics.EmailNotificationMetrics;
import io.micrometer.core.instrument.Timer;
import org.springframework.mail.MailException;

@Service
@RequiredArgsConstructor
public class EmailService implements NotificationService {
    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;
    private final EmailNotificationMetrics emailNotificationMetrics;

    @Override
    public void send(UserDto user, String message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(mailProperties.getUsername());
        mail.setTo(user.getEmail());
        mail.setSubject("Уведомление Dev Social Network");
        mail.setText(message);

        Timer.Sample sample = emailNotificationMetrics.start();
        emailNotificationMetrics.sendingStarted();

        try {
            javaMailSender.send(mail);
        } catch (MailException exception) {
            emailNotificationMetrics.recordError(sample);
            throw exception;
        } finally {
            emailNotificationMetrics.sendingFinished();
        }

        emailNotificationMetrics.recordSuccess(sample);
    }

    @Override
    public UserDto.PreferredContact getPreferredContact() {
        return UserDto.PreferredContact.EMAIL;
    }
}
