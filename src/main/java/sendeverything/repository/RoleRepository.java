package sendeverything.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sendeverything.models.ERole;
import sendeverything.models.Role;

@Repository // @Repository: 將此類別註冊為Spring Bean
public interface RoleRepository extends JpaRepository<Role, Long> {
  Optional<Role> findByName(ERole name);
}
