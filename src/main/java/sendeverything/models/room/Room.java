package sendeverything.models.room;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import sendeverything.models.User;

import java.time.LocalDateTime;

import java.sql.Blob;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "room")
@ToString(exclude = "dbRoomFiles")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String roomCode;
    private String title;
    private String description;

    private String password;
    @Lob
    private Blob image;
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id")
    private User owner;
    private String members;
    @Enumerated(EnumType.STRING)
    private RoomType roomType;
    private LocalDateTime createTime;
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DBRoomFile> dbRoomFiles = new ArrayList<>();

    public Room(String roomCode, String title, String description, String password, Blob image, RoomType roomType, LocalDateTime createTime) {
        this.roomCode = roomCode;
        this.title = title;
        this.description = description;
        this.password = password;
        this.image = image;
        this.roomType = roomType;
        this.createTime = createTime;

    }


    public Room() {

    }
}
