package faang.school.notificationservice.messaging;

import faang.school.notificationservice.dto.event.PostCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class PostCreatedMessageBuilderTest {
    private PostCreatedMessageBuilder messageBuilder;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(false);
        messageBuilder = new PostCreatedMessageBuilder(messageSource);
    }

    @Test
    void shouldDeclareSupportedEventType() {
        assertThat(messageBuilder.getInstance()).isEqualTo(PostCreatedEvent.class);
    }

    @Test
    void shouldBuildRussianMessageWithoutGroupingPostId() {
        String text = messageBuilder.buildMessage(
                new PostCreatedEvent(12345L, 1L, null), Locale.forLanguageTag("ru"));

        assertThat(text).isEqualTo("Твой пост №12345 успешно создан.");
    }

    @Test
    void shouldBuildEnglishMessage() {
        String text = messageBuilder.buildMessage(
                new PostCreatedEvent(12345L, 1L, null), Locale.ENGLISH);

        assertThat(text).isEqualTo("Your post #12345 has been created.");
    }

    @Test
    void shouldUseDefaultBundleForUnsupportedLanguage() {
        String text = messageBuilder.buildMessage(
                new PostCreatedEvent(12345L, 1L, null), Locale.FRENCH);

        assertThat(text).isEqualTo("Your post #12345 has been created.");
    }
}
