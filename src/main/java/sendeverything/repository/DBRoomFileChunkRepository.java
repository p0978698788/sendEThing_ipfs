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



    Optional<DBRoomFileChunk>findByChunkIdAndDbRoomFile_FileIdAndDbRoomFile_Room_RoomCode(String chunkId, String fileId, String roomCode);




}
