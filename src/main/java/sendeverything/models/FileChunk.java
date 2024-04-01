package sendeverything.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@ToString(exclude = "databaseFile")
@EqualsAndHashCode(exclude = "databaseFile")
@NoArgsConstructor
@Table(name = "file_chunks")
public class FileChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String chunkId;
    private String cid;
    private int totalChunks;
    private int chunkNumber;
    @ManyToOne
    @JoinColumn(name = "file_id", nullable = false)
    private DatabaseFile databaseFile;
    public FileChunk(String chunkId,DatabaseFile databaseFile, int totalChunks,int chunkNumber,String cid){
        this.chunkId=chunkId;
        this.databaseFile=databaseFile;
        this.totalChunks=totalChunks;
        this.chunkNumber=chunkNumber;
        this.cid=cid;

    }




}
