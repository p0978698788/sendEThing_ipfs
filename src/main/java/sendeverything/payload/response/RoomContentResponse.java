package sendeverything.payload.response;

import lombok.Data;
import sendeverything.models.room.DBRoomDTO;

import java.util.List;

@Data
public class RoomContentResponse {
    private RoomResponse roomResponse;
    private List<DBRoomDTO> dbRoomFiles;
}
