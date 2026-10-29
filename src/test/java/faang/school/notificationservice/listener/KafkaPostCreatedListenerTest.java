package faang.school.notificationservice.listener;

import faang.school.notificationservice.service.PostCreatedEventHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class KafkaPostCreatedListenerTest {

    @Mock
    private PostCreatedEventHandler handler;

    private KafkaPostCreatedListener listener;

    @BeforeEach
    void setUp() {
        listener = new KafkaPostCreatedListener(handler);
    }

    @Test
    void shouldPassRecordValueToHandler() {
        String body = "{\"postId\":12345,\"authorId\":1,\"projectId\":null}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("post_topic", 0, 5L, "1", body);

        listener.onMessage(record);

        verify(handler).handle(body);
        verifyNoMoreInteractions(handler);
    }

    @Test
    void shouldPropagateProcessingFailureToContainer() {
        String body = "{\"postId\":12345,\"authorId\":1,\"projectId\":null}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("post_topic", 0, 5L, "1", body);
        MailSendException failure = new MailSendException("SMTP unavailable");
        doThrow(failure).when(handler).handle(body);

        assertThatThrownBy(() -> listener.onMessage(record)).isSameAs(failure);
    }
}
