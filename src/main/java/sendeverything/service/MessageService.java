package sendeverything.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sendeverything.models.Message;
import sendeverything.models.User;
import sendeverything.payload.response.FileNameResponse;
import sendeverything.repository.MessageRepository;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;

@Service
public class MessageService {
    @Autowired
    private MessageRepository  messageRepository;

    public Message saveMessage(String content, Optional<User> user) {
        Message message = new Message();
        user.ifPresent(message::setUser);
        message.setContent(content);
        message.setVerificationCode(generateUniqueVerificationCode());
        message.setCreatedAt(LocalDateTime.now());
        return messageRepository.save(message);
    }

    public Message getMessage(String code) {
        return messageRepository.findByVerificationCode(code);
    }

    private String generateUniqueVerificationCode() {
        Random random = new Random();
        String verificationCode;
        do {
            char[] vowels = {'a', 'e', 'i', 'o', 'u'};
            char[] consonants = {'b', 'c', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm', 'n', 'p', 'q', 'r', 's', 't', 'v'};

            StringBuilder codeBuilder = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                // Alternate between consonants and vowels
                if (i % 2 == 0) { // Even index: consonant
                    codeBuilder.append(consonants[random.nextInt(consonants.length)]);
                } else { // Odd index: vowel
                    codeBuilder.append(vowels[random.nextInt(vowels.length)]);
                }
            }
            verificationCode = codeBuilder.toString().toUpperCase(Locale.ROOT);
            System.out.println("Verification code: " + verificationCode);
        } while (isCodeExists(verificationCode));

        return verificationCode;
    }
    private  boolean isCodeExists(String code) {
        return messageRepository.existsByVerificationCode(code);
    }
    public FileNameResponse fileNameResponse(Message message) {
        return new FileNameResponse(message.getVerificationCode());

    }
    public FileNameResponse fileMessageResponse(Message message) {
        return new FileNameResponse(message.getContent());

    }
}
