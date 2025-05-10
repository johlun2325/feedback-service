package task.service.consumers;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import task.service.models.messages.ItemEvent;
import task.service.repos.TaskStatusRepository;
import task.service.services.TaskStatusService;

@ApplicationScoped
public final class ItemEventConsumer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemEventConsumer.class);

    @Inject
    TaskStatusService taskStatusService;

    @Inject
    TaskStatusRepository repository;

    @Inject
    ObjectMapper objectMapper;

    @Incoming("created")
    public void consumeCreated(final String message)
    {
        try
        {
            var event = objectMapper.readValue(message, ItemEvent.class);

            LOGGER.info("Received create event: {}", event.getEvent());
            LOGGER.info("Item create UID: {}", event.getItemUid());

            var taskStatus = taskStatusService.createStatus(event);
            repository.persist(taskStatus);

            var priorityCount = repository.findNotCompletedPriorityByUserUid(event.getUserUid()).size();
            var task = repository.findByUid(event.getItemUid());

            taskStatusService.feedback(event.getUserUid(), event.getEvent(), false, false, task.isCompleted(),
                    priorityCount);
            LOGGER.debug("Processed feedback");

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
            LOGGER.info("Item update UID: {}", event.getItemUid());

            var prevStatus = repository.findByUid(event.getItemUid());

            var wasCompleted = prevStatus.isCompleted();
            var wasPriority = prevStatus.isPriority();

            var updatedStatus = taskStatusService.updateStatus(event, prevStatus);
            repository.update(updatedStatus);

            var priorityCount = repository.findNotCompletedPriorityByUserUid(event.getUserUid()).size();

            taskStatusService.feedback(event.getUserUid(), event.getEvent(), wasCompleted, wasPriority,
                    updatedStatus.isCompleted(), priorityCount);

            LOGGER.debug("Processed feedback");

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
            LOGGER.info("Item delete UID: {}", event.getItemUid());

            var statusToBeDeleted = repository.findByUid(event.getItemUid());

            var wasCompleted = statusToBeDeleted.isCompleted();
            var wasPriority = statusToBeDeleted.isPriority();

            repository.delete(statusToBeDeleted);

            var priorityCount = repository.findNotCompletedPriorityByUserUid(event.getUserUid()).size();

            taskStatusService.feedback(event.getUserUid(), event.getEvent(), wasCompleted, wasPriority,
                    statusToBeDeleted.isCompleted(), priorityCount);

            LOGGER.debug("Processed feedback");

        } catch (final Exception e)
        {
            LOGGER.error("Failed to process item-delete event", e);
        }
    }
}
