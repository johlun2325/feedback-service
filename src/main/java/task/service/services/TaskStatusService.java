package task.service.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import task.service.models.TaskStatus;
import task.service.models.messages.FeedbackEvent;
import task.service.models.messages.ItemEvent;
import task.service.producers.ItemFeedbackProducer;

@ApplicationScoped
public final class TaskStatusService
{
    @Inject
    ItemFeedbackProducer itemFeedbackProducer;

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskStatusService.class);

    public TaskStatus createStatus(final ItemEvent event)
    {
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

    public void feedback(final String userUid, final String eventType, final boolean wasCompleted,
            final boolean wasPriority, final boolean completed, final int priorityCount)
    {

        switch (eventType)
        {
            case "item-created" -> createFeedback(userUid, wasCompleted, wasPriority, completed, priorityCount);
            case "item-updated" -> updateFeedback(userUid, wasCompleted, wasPriority, completed, priorityCount);
            case "item-deleted" -> deleteFeedback(userUid, wasPriority, priorityCount);
        }
    }

    private void createFeedback(final String userUid, final boolean wasCompleted, final boolean wasPriority,
            final boolean completed, int priorityCount)
    {

        if (!wasCompleted && completed)
        {
            LOGGER.debug("Sending feedback event for completed item");

            var event = buildCompletedEvent(userUid);

            try
            {
                itemFeedbackProducer.sendItemCompletedEvent(userUid, event);
            } catch (final Exception e)
            {
                LOGGER.error("Failed to send item completed event for created item", e);
            }
        }

        if (!wasPriority && priorityCount >= 6)
        {
            LOGGER.debug("Sending feedback event for priority, priority count is {}", priorityCount);

            var event = buildPriorityEvent(userUid, priorityCount);

            try
            {
                itemFeedbackProducer.sendItemPriorityEvent(userUid, event);
            } catch (final Exception e)
            {
                LOGGER.error("Failed to send item priority event for created item", e);
            }

        } else
        {
            LOGGER.debug("Priority limit not reached");
        }
    }

    private void updateFeedback(final String userUid, final boolean wasCompleted, final boolean wasPriority,
            final boolean completed, int priorityCount)
    {
        if (!wasCompleted && completed)
        {
            LOGGER.debug("Sending feedback event for newly completed item");

            var event = buildCompletedEvent(userUid);

            try
            {
                itemFeedbackProducer.sendItemCompletedEvent(userUid, event);
            } catch (final Exception e)
            {
                LOGGER.error("Failed to send priority event for updated item", e);
            }

        }

        // todo: only send this feedback if it has changed from under to over etc.
        if (wasPriority && priorityCount >= 6)
        {
            LOGGER.debug("Sending feedback event for priority, priority count is {}", priorityCount);

            var event = buildPriorityEvent(userUid, priorityCount);

            try
            {
                itemFeedbackProducer.sendItemPriorityEvent(userUid, event);
            } catch (final Exception e)
            {
                LOGGER.error("Failed to send priority event for updated item", e);
            }

        } else
        {
            LOGGER.debug("Priority limit not reached");
        }
    }

    private void deleteFeedback(final String userUid, final boolean wasPriority, final int priorityCount)
    {
        // todo: only send this feedback if it has changed from under to over etc.
        if (wasPriority && priorityCount >= 6)
        {
            LOGGER.debug("Sending feedback event for priority, priority count is {}", priorityCount);

            var event = buildPriorityEvent(userUid, priorityCount);
            try
            {
                itemFeedbackProducer.sendItemPriorityEvent(userUid, event);
            } catch (final Exception e)
            {
                LOGGER.error("Failed to send priority event after deleting item", e);
            }

        } else
        {
            LOGGER.debug("Priority limit not reached");
        }
    }

    private FeedbackEvent buildCompletedEvent(final String userUid)
    {
        var event = new FeedbackEvent();
        event.setEvent("feedback-completed");
        event.setType("task");
        event.setUserUid(userUid);
        event.setFeedback("You completed tour task!");
        event.setTime(System.currentTimeMillis());

        return event;
    }

    private FeedbackEvent buildPriorityEvent(final String userUid, final int priorityCount)
    {
        var event = new FeedbackEvent();
        event.setEvent("feedback-completed");
        event.setType("task");
        event.setUserUid(userUid);
        event.setFeedback("Warning! you have " + priorityCount + " priority items!");
        event.setTime(System.currentTimeMillis());

        return event;
    }
}
