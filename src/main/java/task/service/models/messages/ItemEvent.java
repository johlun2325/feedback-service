package task.service.models.messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class ItemEvent
{
    private String event;
    private String itemUid;
    private String userUid;
    private String type;
    private Map<String, Object> content;
    private long time;
}
