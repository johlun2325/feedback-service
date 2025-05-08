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
public class ItemEventConsumer
{

    private static final Logger LOGGER = LoggerFactory.getLogger(ItemEventConsumer.class);

    @Inject
    TaskStatusService taskStatusService;

    @Inject
    TaskStatusRepository repository;

    @Inject
    ObjectMapper objectMapper;

    @Incoming("created")
    public void consumeCreated(final String json)
    {

        try
        {
            var event = objectMapper.readValue(json, ItemEvent.class);
            LOGGER.info("Received create event: {}", event.getEvent());
            LOGGER.info("Item create UID: {}", event.getItemUid());

            var taskStatus = taskStatusService.createStatus(event);
            repository.persist(taskStatus);

            var priority = repository.findPriorityByUserUid(event.getUserUid()).size();
            var completed = repository.findCompletedByUserUid(event.getUserUid()).size();

            taskStatusService.feedback(event.getType(), taskStatus, priority, completed);

        } catch (final Exception e)
        {
            LOGGER.error("Failed to process item-created event", e);
        }
    }

    @Incoming("updated")
    public void consumeUpdated(final String json)
    {
        try
        {
            var event = objectMapper.readValue(json, ItemEvent.class);
            LOGGER.info("Received update event: {}", event.getEvent());
            LOGGER.info("Item update UID: {}", event.getItemUid());

            var prevStatus = repository.findByUid(event.getItemUid());
            var taskStatus = taskStatusService.updateStatus(event, prevStatus);

            repository.update(taskStatus);

            var priority = repository.findPriorityByUserUid(event.getUserUid()).size();
            var completed = repository.findCompletedByUserUid(event.getUserUid()).size();

            taskStatusService.feedback(event.getType(), prevStatus, priority, completed);

        } catch (final Exception e)
        {
            LOGGER.error("Failed to process item-update event", e);
        }
    }

    @Incoming("deleted")
    public void consumeDeleted(final String json)
    {
        try
        {
            var event = objectMapper.readValue(json, ItemEvent.class);
            LOGGER.info("Received delete event: {}", event.getEvent());
            LOGGER.info("Item delete UID: {}", event.getItemUid());

            var prevStatus = repository.findByUid(event.getItemUid());
            var taskStatus = taskStatusService.updateStatus(event, prevStatus);

            repository.delete(taskStatus);

            var priority = repository.findPriorityByUserUid(event.getUserUid()).size();
            var completed = repository.findCompletedByUserUid(event.getUserUid()).size();

            taskStatusService.feedback(event.getType(), prevStatus, priority, completed);

        } catch (final Exception e)
        {
            LOGGER.error("Failed to process item-delete event", e);
        }
    }
}
