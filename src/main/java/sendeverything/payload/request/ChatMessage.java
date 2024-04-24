package sendeverything.payload.request;

import lombok.*;
import sendeverything.models.MessageType;
import sendeverything.models.User;

import java.sql.Blob;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessage {
    private MessageType type;
    private String content;
    private String sender;

}