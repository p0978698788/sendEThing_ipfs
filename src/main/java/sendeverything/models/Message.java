package sendeverything.models;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob  // 标记为大对象
    @Column(name = "content", nullable = false,columnDefinition = "TEXT")
    private String content;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    private String verificationCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 省略构造函数、getter 和 setter 方法
}