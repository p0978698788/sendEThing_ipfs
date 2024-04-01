package sendeverything.models.room;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import sendeverything.models.room.DBRoomFile;

@Entity
@Data
@ToString(exclude = "dbRoomFile")
@EqualsAndHashCode(exclude = "dbRoomFile")
@NoArgsConstructor
@Table(name = "room_file_chunks")
public class DBRoomFileChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String chunkId;
    private String cid;
    private int totalChunks;
    private int chunkNumber;
    @ManyToOne
    @JoinColumn(name = "file_id", nullable = false)
    private DBRoomFile dbRoomFile;
    public DBRoomFileChunk(String chunkId,DBRoomFile dbRoomFile, int totalChunks,int chunkNumber,String cid){
        this.chunkId=chunkId;
        this.dbRoomFile=dbRoomFile;
        this.totalChunks=totalChunks;
        this.chunkNumber=chunkNumber;
        this.cid=cid;

    }




}