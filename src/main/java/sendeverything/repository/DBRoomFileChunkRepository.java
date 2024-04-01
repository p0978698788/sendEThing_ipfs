package sendeverything.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sendeverything.models.DatabaseFile;
import sendeverything.models.FileChunk;
import sendeverything.models.room.DBRoomFile;
import sendeverything.models.room.DBRoomFileChunk;

import java.util.List;
import java.util.Optional;

@Repository
public interface DBRoomFileChunkRepository extends JpaRepository<DBRoomFileChunk, Long> {
    Optional<DBRoomFileChunk> findByChunkId(String ChunkId);
    List<DBRoomFileChunk> findByDbRoomFileOrderByChunkNumberAsc(DBRoomFile dbRoomFile);



    Optional<DBRoomFileChunk> findByChunkIdAndDbRoomFile_FileId(String chunkId, String fileId);



//    @Query("SELECT fc.chunkNumber AS chunkNumber, fc.eTag AS eTag FROM FileChunk fc WHERE fc.databaseFile.fileId = :fileId ORDER BY fc.chunkNumber ASC")
//    List<FileChunkProjection> findChunkNumberAndETagByDatabaseFile_FileIdOrderByChunkNumberAsc(@Param("fileId") String fileId);

//    List<FileChunk> testFindChunksByDatabaseFile_FileId(@Param("fileId") String fileId);

}
