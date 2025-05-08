package task.service.models;

import lombok.Data;

@Data
public final class Task
{
    private String uid;
    private String userUid;
    private String type;
    private String title;
    private String description;
    private boolean priority;
    private boolean completed;
    private Long createdAt;
    private Long updatedAt;
    private Long completedAt;

    @Override
    public String toString()
    {
        return "Task{" + "\n uid='" + uid + '\'' + "\n, userUid='" + userUid + '\'' + "\n, title='" + title + '\''
                + "\n, description='" + description + '\'' + "\n, priority=" + priority + "\n, completed=" + completed
                + "\n, createdAt=" + createdAt + "\n, updatedAt=" + updatedAt + "\n, completedAt=" + completedAt + '}';
    }
}
