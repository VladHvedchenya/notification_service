package faang.school.notificationservice.service.mail;

import faang.school.notificationservice.dto.UserDto;
import faang.school.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService implements NotificationService {
    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;

    @Override
    public void send(UserDto user, String message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(mailProperties.getUsername());
        mail.setTo(user.getEmail());
        mail.setSubject("Уведомление CorporationX");
        mail.setText(message);
        javaMailSender.send(mail);
    }

    @Override
    public UserDto.PreferredContact getPreferredContact() {
        return UserDto.PreferredContact.EMAIL;
    }
}
