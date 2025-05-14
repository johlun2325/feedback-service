package task.service.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import task.service.models.messages.ItemEvent;
import task.service.repos.TaskStatusRepository;
import task.service.services.FeedbackService;

@ApplicationScoped
public final class ItemEventConsumer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemEventConsumer.class);

    @Inject
    FeedbackService feedbackService;

    @Inject
    TaskStatusRepository repository;

    @Inject
    ObjectMapper objectMapper;

    @Incoming("created")
    public void consumeCreated(final String jsonMsg)
    {
        try
        {
            var event = objectMapper.readValue(jsonMsg, ItemEvent.class);
            LOGGER.info("Received create event: {}", event.getEvent());

            var taskStatus = feedbackService.createStatus(event);
            repository.persist(taskStatus);

            var priorityCount = repository.findNotCompletedPriorityByUserUid(event.getUserUid()).size();
            var completedCount = repository.findCompletedByUserUid(event.getUserUid()).size();

            var task = repository.findByUid(event.getItemUid());

            feedbackService.feedback(event.getUserUid(), event.getEvent(), task.isCompleted(), priorityCount,
                    completedCount);
        } catch (final Exception e)
        {
            LOGGER.error("Failed to process item-created event", e);
        }
    }

    @Incoming("updated")
    public void consumeUpdated(final String message)
    {
        try
        {
            var event = objectMapper.readValue(message, ItemEvent.class);
            LOGGER.info("Received update event: {}", event.getEvent());

            var prevStatus = repository.findByUid(event.getItemUid());

            var updatedStatus = feedbackService.updateStatus(event, prevStatus);
            repository.update(updatedStatus);

            var priorityCount = repository.findNotCompletedPriorityByUserUid(event.getUserUid()).size();
            var completedCount = repository.findCompletedByUserUid(event.getUserUid()).size();

            feedbackService.feedback(event.getUserUid(), event.getEvent(), updatedStatus.isCompleted(), priorityCount,
                    completedCount);

        } catch (final Exception e)
        {
            LOGGER.error("Failed to process item-update event", e);
        }
    }

    @Incoming("deleted")
    public void consumeDeleted(final String message)
    {
        try
        {
            var event = objectMapper.readValue(message, ItemEvent.class);
            LOGGER.info("Received delete event: {}", event.getEvent());

            var statusToBeDeleted = repository.findByUid(event.getItemUid());

            repository.delete(statusToBeDeleted);

            var priorityCount = repository.findNotCompletedPriorityByUserUid(event.getUserUid()).size();
            var completedCount = repository.findCompletedByUserUid(event.getUserUid()).size();

            feedbackService.feedback(event.getUserUid(), event.getEvent(), statusToBeDeleted.isCompleted(),
                    priorityCount, completedCount);

        } catch (final Exception e)
        {
            LOGGER.error("Failed to process item-delete event", e);
        }
    }
}
