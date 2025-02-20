package sendeverything.payload.request;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import sendeverything.models.MessageType;
import sendeverything.models.User;

import java.sql.Blob;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessage {
    private MessageType type;
    private String content;
    private String sender;
    private String roomCode;

}