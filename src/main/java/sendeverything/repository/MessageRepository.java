package sendeverything.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sendeverything.models.Message;

import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {
    boolean existsByVerificationCode(String code);
    Message findByVerificationCode(String code);
    // 可以在这里添加自定义查询方法
}