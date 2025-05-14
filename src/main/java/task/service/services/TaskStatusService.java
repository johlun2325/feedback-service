package task.service.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import task.service.models.TaskStatus;
import task.service.models.messages.FeedbackEvent;
import task.service.models.messages.ItemEvent;
import task.service.producers.ItemFeedbackProducer;

import java.util.UUID;

@ApplicationScoped
public final class TaskStatusService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskStatusService.class);
    private static final int PRIORITY_LIMIT = 5;
    private static final String PRIORITY_ERROR_MSG = "Failed to send priority event";
    private static final String COMPLETED_ERROR_MSG = "Failed to send completed event";

    @Inject
    ItemFeedbackProducer itemFeedbackProducer;

    public TaskStatus createStatus(final ItemEvent event)
    {
        LOGGER.debug("Creating status for item: {} and user {}", event.getItemUid(), event.getUserUid());

        if (event.getType() == null)
        {
            return new TaskStatus();
        }

        var status = new TaskStatus();
        var content = event.getContent();

        status.setUid(event.getItemUid());
        status.setUserUid(event.getUserUid());
        status.setType(event.getType());
        status.setPriority(content.containsKey("priority") && (boolean) content.get("priority"));
        status.setCompleted(content.containsKey("completed") && (boolean) content.get("completed"));
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
        LOGGER.debug("Updating status for item: {} and user {}", event.getItemUid(), event.getUserUid());

        var content = event.getContent();

        prevStatus.setPriority(content.containsKey("priority") && (boolean) content.get("priority"));
        prevStatus.setCompleted(content.containsKey("completed") && (boolean) content.get("completed"));
        prevStatus.setUpdatedAt(content.containsKey("time") ? (long) content.get("time") : event.getTime());

        if (event.getType().equals("completed"))
        {
            prevStatus.setCompletedAt(event.getTime());
        }

        return prevStatus;
    }

    public void feedback(final String userUid, final String eventType, final boolean completed, final int priorityCount,
            final int completedCount)
    {
        switch (eventType)
        {
            case "item-created", "item-updated" -> {
                sendCompletedFeedback(userUid, completed, completedCount);
                sendPriorityFeedback(userUid, priorityCount);
            }
            case "item-deleted" -> sendPriorityFeedback(userUid, priorityCount);
        }
    }

    private FeedbackEvent buildCompletedEvent(final boolean completed, final int completedCount, final String userUid)
    {
        var event = new FeedbackEvent();
        event.setEvent("feedback-completed");
        event.setFeedbackUid(UUID.randomUUID().toString());
        event.setType("task");
        event.setUserUid(userUid);

        if (completed)
        {
            event.setFeedback("Yes! You completed your task! Your total is: " + completedCount + "!");
        } else
        {
            event.setFeedback("I believe in you! Go do stuff!");
        }

        event.setTime(System.currentTimeMillis());

        return event;
    }

    private FeedbackEvent buildPriorityEvent(final String userUid, final int priorityCount)
    {
        var event = new FeedbackEvent();
        event.setEvent("feedback-priority");
        event.setFeedbackUid(UUID.randomUUID().toString());
        event.setType("task");
        event.setUserUid(userUid);

        if (priorityCount >= PRIORITY_LIMIT)
        {
            event.setFeedback("Warning! you have " + priorityCount + " priority items!");
        } else
        {
            event.setFeedback("Priority is under under control");
        }

        event.setTime(System.currentTimeMillis());

        return event;
    }

    private void sendCompletedFeedback(final String userUid, final boolean completed, final int completedCount)
    {
        var event = buildCompletedEvent(completed, completedCount, userUid);

        try
        {
            itemFeedbackProducer.sendItemCompletedEvent(userUid, event);
        } catch (final Exception e)
        {
            LOGGER.error(COMPLETED_ERROR_MSG, e);
        }
    }

    private void sendPriorityFeedback(final String userUid, final int priorityCount)
    {
        var event = buildPriorityEvent(userUid, priorityCount);

        try
        {
            itemFeedbackProducer.sendItemPriorityEvent(userUid, event);
        } catch (final Exception e)
        {
            LOGGER.error(PRIORITY_ERROR_MSG, e);
        }
    }
}
