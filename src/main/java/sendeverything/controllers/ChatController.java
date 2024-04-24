package sendeverything.controllers;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import sendeverything.payload.request.ChatMessage;

@Controller
public class ChatController {

    @MessageMapping("/chat.sendMessage/{roomCode}")
    @SendTo("/topic/{roomCode}")
    public ChatMessage sendMessage(
            @PathVariable String roomCode,
            @Payload ChatMessage chatMessage
    ) {
//        System.out.println(chatMessage.getType());
//        System.out.println(chatMessage.getSender());
//        System.out.println(chatMessage.getContent());
        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/{roomCode}")
    public ChatMessage addUser(
            @PathVariable String roomCode,
            @Payload ChatMessage chatMessage,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        // Add username in web socket session
//        System.out.println(chatMessage);
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());

        return chatMessage;
    }

}