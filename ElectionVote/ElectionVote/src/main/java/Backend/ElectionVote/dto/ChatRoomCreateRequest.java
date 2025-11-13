package Backend.ElectionVote.dto;

import Backend.ElectionVote.enums.RoomType;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ChatRoomCreateRequest {
    private String name;
    private String description;
    private RoomType roomType;
    private Map<String, Object> settings;
}