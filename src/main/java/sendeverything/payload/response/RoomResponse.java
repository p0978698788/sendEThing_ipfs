package sendeverything.payload.response;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sendeverything.models.User;
import sendeverything.models.room.DBRoomFile;
import sendeverything.models.room.RoomType;

import java.sql.Blob;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoomResponse {
    private String roomCode;
    private String title;
    private String description;
    private String image;
    private RoomType roomType;
    private LocalDateTime createTime;
    private Boolean isOwner;
    private List<DBRoomFile> dbRoomFiles = new ArrayList<>();

}
