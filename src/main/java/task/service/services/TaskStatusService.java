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
        return new TaskStatus();
    }

    public TaskStatus updateStatus(final ItemEvent event, final TaskStatus prevStatus)
    {
        return new TaskStatus();
    }

    public void feedback(final String eventType, final TaskStatus taskStatus, final int priority, final int completed)
    {
        if (!eventType.equals("item-delete"))
        {
            if (taskStatus.isCompleted())
            {
                LOGGER.info("Task is completed");
            }
        }

        if (priority >= 10)
        {
            // todo: send feedback event
            LOGGER.info("Sending feedback event for priority, priority count is{}", priority);
        }

        if (taskStatus.isCompleted() && completed == 5)
        {
            // todo: send feedback event
            LOGGER.info("Sending feedback event for completed, completed count is{}", priority);

        }
    }
}
