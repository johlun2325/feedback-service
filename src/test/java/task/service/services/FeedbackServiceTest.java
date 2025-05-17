package task.service.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import task.service.models.TaskStatus;
import task.service.models.messages.FeedbackEvent;
import task.service.models.messages.ItemEvent;
import task.service.producers.ItemFeedbackProducer;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

public class FeedbackServiceTest
{
    private FeedbackService feedbackService;

    @Mock
    ItemFeedbackProducer itemFeedbackProducer;

    private AutoCloseable mocks;

    @BeforeEach
    void setup()
    {
        mocks = MockitoAnnotations.openMocks(this);
        feedbackService = new FeedbackService();
        feedbackService.itemFeedbackProducer = itemFeedbackProducer;
    }

    @AfterEach
    void tearDown() throws Exception
    {
        mocks.close();
    }

    @Test
    void createStatusWithCompletedTrue()
    {
        var now = System.currentTimeMillis();
        var event = new ItemEvent();
        event.setItemUid("item123");
        event.setUserUid("user456");
        event.setType("completed");
        event.setTime(now);
        event.setContent(Map.of("priority", true, "completed", true));

        var status = feedbackService.createStatus(event);

        assertEquals("item123", status.getUid());
        assertEquals("user456", status.getUserUid());
        assertEquals("completed", status.getType());
        assertTrue(status.isPriority());
        assertTrue(status.isCompleted());
        assertEquals(now, status.getCreatedAt());
        assertEquals(now, status.getUpdatedAt());
        assertEquals(now, status.getCompletedAt()); // the timestamp should be set
    }

    @Test
    void ceateStatus()
    {
        var now = System.currentTimeMillis();
        var event = new ItemEvent();
        event.setItemUid("itemABC");
        event.setUserUid("userDEF");
        event.setType("task");
        event.setTime(now);
        event.setContent(Map.of("priority", true, "completed", false));

        var status = feedbackService.createStatus(event);

        assertEquals("itemABC", status.getUid());
        assertEquals("userDEF", status.getUserUid());
        assertEquals("task", status.getType());
        assertTrue(status.isPriority());
        assertFalse(status.isCompleted());
        assertEquals(now, status.getCreatedAt());
        assertEquals(now, status.getUpdatedAt());
        assertNull(status.getCompletedAt()); // should be null if not completed
    }

    @Test
    void updateStatusCompletedTrue()
    {
        var now = System.currentTimeMillis();
        var event = new ItemEvent();
        event.setItemUid("item123");
        event.setUserUid("user456");
        event.setTime(now);
        event.setContent(Map.of("priority", false, "completed", true));

        var prevStatus = new TaskStatus();
        prevStatus.setUid("item123");
        prevStatus.setUserUid("user456");
        prevStatus.setPriority(false);
        prevStatus.setCompleted(false);
        prevStatus.setCreatedAt(now - 1000);
        prevStatus.setUpdatedAt(now - 1000);

        var updated = feedbackService.updateStatus(event, prevStatus);

        assertFalse(updated.isPriority());
        assertTrue(updated.isCompleted());
        assertEquals(now, updated.getUpdatedAt());
        assertEquals(now, updated.getCompletedAt());
    }

    @Test
    void updateStatusWithCustomTime()
    {
        var eventTime = System.currentTimeMillis();
        var customTime = eventTime + 5000;

        var event = new ItemEvent();
        event.setItemUid("item789");
        event.setUserUid("userXYZ");
        event.setTime(eventTime);
        event.setContent(Map.of("priority", true, "completed", false, "time", customTime));

        var prevStatus = new TaskStatus();
        prevStatus.setUid("item789");
        prevStatus.setUserUid("userXYZ");
        prevStatus.setPriority(false);
        prevStatus.setCompleted(true);
        prevStatus.setCreatedAt(eventTime - 10000);
        prevStatus.setUpdatedAt(eventTime - 10000);

        var updated = feedbackService.updateStatus(event, prevStatus);

        assertTrue(updated.isPriority());
        assertFalse(updated.isCompleted());
        assertEquals(customTime, updated.getUpdatedAt());
        assertNull(updated.getCompletedAt());
    }

    @Test
    void feedbackItemCreated()
    {
        var userUid = "user123";
        var eventType = "item-created";
        var completed = true;
        var completedCount = 6; // over current priority limit
        var priorityCount = 3;

        feedbackService.feedback(userUid, eventType, completed, priorityCount, completedCount);

        verify(itemFeedbackProducer).sendItemCompletedEvent(eq(userUid), any(FeedbackEvent.class));
        verify(itemFeedbackProducer).sendItemPriorityEvent(eq(userUid), any(FeedbackEvent.class));
    }

    @Test
    void feedbackItemUpdated()
    {
        String userUid = "user123";
        String eventType = "item-updated";
        boolean completed = false;
        int completedCount = 3;
        int priorityCount = 2;

        feedbackService.feedback(userUid, eventType, completed, priorityCount, completedCount);

        verify(itemFeedbackProducer).sendItemCompletedEvent(eq(userUid), any(FeedbackEvent.class));
        verify(itemFeedbackProducer).sendItemPriorityEvent(eq(userUid), any(FeedbackEvent.class));
    }

    @Test
    void buildCompletedEventTrue()
    {
        var completed = true;
        var completedCount = 5;
        var userUid = "user123";

        var event = feedbackService.buildCompletedEvent(completed, completedCount, userUid);

        assertNotNull(event);
        assertEquals("feedback-completed", event.getEvent());
        assertEquals("Yes! You completed your task! Your total is 5", event.getFeedback());
        assertEquals(userUid, event.getUserUid());
    }

    @Test
    void buildCompletedEventFalse()
    {
        var completed = false;
        var completedCount = 3;
        var userUid = "user123";

        var event = feedbackService.buildCompletedEvent(completed, completedCount, userUid);

        assertNotNull(event);
        assertEquals("feedback-completed", event.getEvent());
        assertEquals("I believe in you! Go do stuff!", event.getFeedback());
        assertEquals(userUid, event.getUserUid());
    }

    @Test
    void buildPriorityEvent()
    {
        var userUid = "user123";
        var priorityCountLow = 2; // under limit
        var priorityCountHigh = 5; // over limit

        var eventLow = feedbackService.buildPriorityEvent(userUid, priorityCountLow);
        var eventHigh = feedbackService.buildPriorityEvent(userUid, priorityCountHigh);

        assertNotNull(eventLow);
        assertEquals("feedback-priority", eventLow.getEvent());
        assertEquals("Priority is under under control", eventLow.getFeedback());

        assertNotNull(eventHigh);
        assertEquals("feedback-priority", eventHigh.getEvent());
        assertEquals("Warning! You have 5 priority items!", eventHigh.getFeedback());
    }
}
