package task.service.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.mongodb.panache.common.MongoEntity;
import lombok.Data;
import org.bson.BsonType;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonRepresentation;

@Data
@MongoEntity(collection = "tasks")
public class TaskStatus
{
    @JsonIgnore
    @BsonId
    @BsonRepresentation(BsonType.OBJECT_ID)
    private String id;

    private String uid;
    private String userUid;
    private String type;
    private boolean priority;
    private boolean completed;
    private Long createdAt;
    private Long updatedAt;
    private Long completedAt;

    @Override
    public String toString()
    {
        return "Task{" + "\n uid='" + uid + '\'' + "\n, userUid='" + userUid + '\'' + "\n, priority=" + priority
                + "\n, completed=" + completed + "\n, createdAt=" + createdAt + "\n, updatedAt=" + updatedAt
                + "\n, completedAt=" + completedAt + '}';
    }
}
