package sendeverything.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sendeverything.models.DatabaseFile;
import sendeverything.models.FileChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileChunkRepository  extends JpaRepository<FileChunk, Long> {
    Optional<FileChunk> findByChunkId(String ChunkId);
    List<FileChunk> findByDatabaseFileOrderByChunkNumberAsc(DatabaseFile databaseFile);



    Optional<FileChunk> findByChunkIdAndDatabaseFile_FileId(String chunkId, String fileId);



//    @Query("SELECT fc.chunkNumber AS chunkNumber, fc.eTag AS eTag FROM FileChunk fc WHERE fc.databaseFile.fileId = :fileId ORDER BY fc.chunkNumber ASC")
//    List<FileChunkProjection> findChunkNumberAndETagByDatabaseFile_FileIdOrderByChunkNumberAsc(@Param("fileId") String fileId);

//    List<FileChunk> testFindChunksByDatabaseFile_FileId(@Param("fileId") String fileId);


}



