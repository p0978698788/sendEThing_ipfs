package sendeverything.models.room;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import sendeverything.models.User;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Data
@ToString(exclude = "dbRoomFileChunks")
@EqualsAndHashCode(exclude = "dbRoomFileChunks")
@Table(name = "room_files")
public class DBRoomFile {
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id;
    private Long fileSize;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;


    private String fileName;
    private String fileId;
    @OneToMany(mappedBy = "dbRoomFile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("chunkNumber ASC")
    private List<DBRoomFileChunk> dbRoomFileChunks = new ArrayList<>();
    private String verificationCode;

    @OrderBy("timestamp DESC")
    LocalDateTime timestamp ;

    private String shortUrl;
    private String description;
    private String uploadId;
    @Column(name = "`key`")
    private String key;
    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    //	private Uploader uploader;




    public DBRoomFile() {

    }

    public DBRoomFile(String fileName, String fileId, LocalDateTime timestamp) {
        this.fileName = fileName;
        this.fileId = fileId;
        this.timestamp = timestamp;
    }





}