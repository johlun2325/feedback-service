package task.service.producers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import task.service.models.messages.FeedbackEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class ItemFeedbackProducerTest
{

    private ItemFeedbackProducer producer;
    private Emitter<String> itemCompletedEmitterMock;
    private Emitter<String> itemPriorityEmitterMock;
    private ObjectMapper objectMapperMock;

    @BeforeEach
    void setUp()
    {
        producer = new ItemFeedbackProducer();

        itemCompletedEmitterMock = mock(Emitter.class);
        itemPriorityEmitterMock = mock(Emitter.class);
        objectMapperMock = mock(ObjectMapper.class);

        producer.setItemCompletedEmitter(itemCompletedEmitterMock);
        producer.setItemPriorityEmitter(itemPriorityEmitterMock);
        producer.setObjectMapper(objectMapperMock);
    }

    @Test
    void sendItemCompletedEvent() throws JsonProcessingException
    {
        // verify serialization and being sent correctly

        // Arrange
        var key = "completedKey";
        var event = new FeedbackEvent("completed", "fb-123-456", "user-123", "task", "Completed!",
                System.currentTimeMillis());

        var serializedEvent = "{\"event\":\"completed\",\"feedbackUid\":\"fb-123-456\",\"userUid\":\"user-789\",\"type\":\"task\",\"feedback\":\"Completed!\",\"time\":1621234567890}";

        when(objectMapperMock.writeValueAsString(event)).thenReturn(serializedEvent);

        // Act
        producer.sendItemCompletedEvent(key, event);

        // Assert
        ArgumentCaptor<KafkaRecord<String, String>> recordCaptor = ArgumentCaptor.forClass(KafkaRecord.class);
        verify(itemCompletedEmitterMock).send(recordCaptor.capture());

        KafkaRecord<String, String> capturedRecord = recordCaptor.getValue();
        assertEquals(key, capturedRecord.getKey());
        assertEquals(serializedEvent, capturedRecord.getPayload());

    }

    @Test
    void sendItemCompletedEvent_shouldThrow() throws JsonProcessingException
    {
        // Arrange
        var key = "completedKey";
        FeedbackEvent event = new FeedbackEvent("completed", "fb-123-456", "user-234", "task", "Feedback",
                System.currentTimeMillis());

        when(objectMapperMock.writeValueAsString(event)).thenThrow(new JsonProcessingException("Test exception") {
        });

        // Act & Assert
        assertThrows(RuntimeException.class, () -> producer.sendItemCompletedEvent(key, event));

        verify(itemCompletedEmitterMock, never()).send(any(KafkaRecord.class));
    }

    @Test
    void sendItemPriorityEvent() throws JsonProcessingException
    {
        // verify serialization and being sent correctly

        // Arrange
        var key = "priorityKey";
        FeedbackEvent event = new FeedbackEvent("priority", "fb-789-012", "user-345", "priority", "High priority",
                System.currentTimeMillis());

        var serializedEvent = "{\"event\":\"priority\",\"feedbackUid\":\"fb-789-012\",\"userUid\":\"user-345\",\"type\":\"priority\",\"feedback\":\"High priority\",\"time\":1621234567890}";

        when(objectMapperMock.writeValueAsString(event)).thenReturn(serializedEvent);

        // Act
        producer.sendItemPriorityEvent(key, event);

        // Assert
        ArgumentCaptor<KafkaRecord<String, String>> recordCaptor = ArgumentCaptor.forClass(KafkaRecord.class);
        verify(itemPriorityEmitterMock).send(recordCaptor.capture());

        KafkaRecord<String, String> capturedRecord = recordCaptor.getValue();
        assertEquals(key, capturedRecord.getKey());
        assertEquals(serializedEvent, capturedRecord.getPayload());
    }

    @Test
    void sendItemPriorityEvent_shouldThrow() throws JsonProcessingException
    {
        // Arrange
        var key = "priorityKey";
        FeedbackEvent event = new FeedbackEvent("priority", "fb-123-456", "user-456", "task", "Feedback",
                System.currentTimeMillis());

        when(objectMapperMock.writeValueAsString(event)).thenThrow(new JsonProcessingException("Test exception") {
        });

        // Act & Assert
        assertThrows(RuntimeException.class, () -> producer.sendItemCompletedEvent(key, event));

        verify(itemCompletedEmitterMock, never()).send(any(KafkaRecord.class));
    }
}
