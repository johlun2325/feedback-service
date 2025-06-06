package task.service.repos;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import task.service.models.TaskStatus;

import java.util.List;

@ApplicationScoped
public final class TaskStatusRepository implements PanacheMongoRepository<TaskStatus>
{
    public TaskStatus findByUid(final String uid)
    {
        return find("uid", uid).firstResult();
    }

    public List<TaskStatus> findCompletedByUserUid(final String userUid)
    {
        return list("userUid = ?1 and completed = ?2", userUid, true);
    }

    public List<TaskStatus> findNotCompletedPriorityByUserUid(final String userUid)
    {
        return list("userUid = ?1 and priority = ?2 and completed = ?3", userUid, true, false);
    }
}
