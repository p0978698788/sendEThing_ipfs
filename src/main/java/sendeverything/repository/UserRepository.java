package sendeverything.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sendeverything.models.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByUsername(String username);// Optional的用途為：當你不確定一個物件是否為null時，你可以使用Optional來表示。

  Optional<User> findByEmail(String email);


  Boolean existsByUsername(String username);

  Boolean existsByEmail(String email);
  @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email AND u.provider = 'LOCAL'")
  Boolean findProviderByEmail(@Param("email") String email);

}
