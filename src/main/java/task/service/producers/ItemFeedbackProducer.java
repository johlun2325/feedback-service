package task.service.producers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.kafka.KafkaRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import task.service.models.messages.FeedbackEvent;

@ApplicationScoped
public final class ItemFeedbackProducer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemFeedbackProducer.class);

    @Inject
    @Channel("completed")
    Emitter<String> itemCompletedEmitter;

    @Inject
    @Channel("priority")
    Emitter<String> itemPriorityEmitter;

    @Inject
    ObjectMapper objectMapper;

    public void sendItemCompletedEvent(final String key, final FeedbackEvent event)
    {
        try
        {
            LOGGER.debug("Sending item completed event");

            var json = objectMapper.writeValueAsString(event);
            itemCompletedEmitter.send(KafkaRecord.of(key, json));

        } catch (final Exception e)
        {
            LOGGER.error("Failed to send item completed event", e);
            throw new RuntimeException("Failed to send event", e);
        }
    }

    public void sendItemPriorityEvent(final String key, final FeedbackEvent event)
    {
        try
        {
            LOGGER.debug("Sending item priority event");
            var json = objectMapper.writeValueAsString(event);
            itemPriorityEmitter.send(KafkaRecord.of(key, json));

        } catch (final Exception e)
        {
            LOGGER.error("Failed to send item priority event", e);
        }
    }
}
