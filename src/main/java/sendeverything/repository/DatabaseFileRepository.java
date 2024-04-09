package sendeverything.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sendeverything.models.DatabaseFile;
import sendeverything.models.FileNameAndVerifyCodeProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface DatabaseFileRepository extends JpaRepository<DatabaseFile, String> {

    @Query("SELECT df.fileName FROM DatabaseFile df WHERE df.user.id = :userId ORDER BY df.timestamp DESC")
    List<String> findFileNamesByUserIdOrderByTimestampDesc(@Param("userId") Long userId);

    List<FileNameAndVerifyCodeProjection> findAllProjectedBy();




    Optional<DatabaseFile> findByFileName(String fileName);

    Optional<DatabaseFile> findByFileId(String fileId);



    Optional<DatabaseFile> findByVerificationCode(String verificationCode);
    void deleteById(String id);





    List<DatabaseFile> findByUserIdIsNullAndTimestampBefore(Instant time);

}