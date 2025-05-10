package task.service.services;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import task.service.models.TaskStatus;
import task.service.models.messages.ItemEvent;

@ApplicationScoped
public class TaskStatusService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskStatusService.class);

    public TaskStatus createStatus(final ItemEvent event)
    {
        if (event.getType() == null)
        {
            return new TaskStatus();
        }

        var status = new TaskStatus();
        status.setUid(event.getItemUid());
        status.setUserUid(event.getUserUid());
        status.setType(event.getType());
        status.setPriority(event.getType().equals("priority"));
        status.setCompleted(event.getType().equals("completed"));
        status.setCreatedAt(event.getTime());
        status.setUpdatedAt(event.getTime());

        if (event.getType().equals("completed"))
        {
            status.setCompletedAt(event.getTime());
        }

        return status;
    }

    public TaskStatus updateStatus(final ItemEvent event, final TaskStatus prevStatus)
    {
        var status = prevStatus;
        var content = event.getContent();

        status.setPriority(content.containsKey("priority") && (boolean) content.get("priority"));
        status.setCompleted(content.containsKey("completed") && (boolean) content.get("completed"));
        status.setUpdatedAt(content.containsKey("time") ? (long) content.get("time") : event.getTime());

        if (event.getType().equals("completed"))
        {
            status.setCompletedAt(event.getTime());
        }

        return status;
    }

    public void feedback(final String eventType, final boolean wasCompleted, final boolean wasPriority,
            final boolean completed, final int priorityCount)
    {

        switch (eventType)
        {
            case "item-created" -> createFeedback(wasCompleted, wasPriority, completed, priorityCount);
            case "item-updated" -> updateFeedback(wasCompleted, wasPriority, completed, priorityCount);
            case "item-deleted" -> deleteFeedback(wasPriority, priorityCount);
        }
    }

    private void createFeedback(final boolean wasCompleted, final boolean wasPriority, final boolean completed,
            int priorityCount)
    {

        if (!wasCompleted && completed)
        {
            // todo: send feedback event
            LOGGER.info("Sending feedback event for completed item");
        }

        if (!wasPriority && priorityCount >= 6)
        {
            // todo: send feedback event
            LOGGER.info("Sending feedback event for priority limit is reached");
        } else
        {
            LOGGER.debug("Priority limit not reached");
        }
    }

    private void updateFeedback(final boolean wasCompleted, final boolean wasPriority, final boolean completed,
            int priorityCount)
    {
        if (!wasCompleted && completed)
        {
            // todo: send feedback event
            LOGGER.info("Sending feedback event for newly completed item");
        }

        // todo: only send this feedback if it has changed from under to over etc.
        if (wasPriority && priorityCount >= 6)
        {
            // todo: send feedback event
            LOGGER.info("Sending feedback event for priority limit is reached");
        } else
        {
            LOGGER.debug("Priority limit not reached");
        }
    }

    private void deleteFeedback(final boolean wasPriority, final int priorityCount)
    {
        // todo: only send this feedback if it has changed from under to over etc.
        if (wasPriority && priorityCount >= 6)
        {
            // todo: send feedback event
            LOGGER.info("Sending feedback event for priority, priority count is {}", priorityCount);
        } else
        {
            LOGGER.debug("Priority limit not reached");
        }
    }
}
