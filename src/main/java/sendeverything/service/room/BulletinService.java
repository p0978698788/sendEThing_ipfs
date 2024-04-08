package sendeverything.service.room;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sendeverything.exception.RoomNotFoundException;
import sendeverything.models.User;
import sendeverything.models.room.Room;
import sendeverything.models.room.RoomType;
import sendeverything.models.room.UserRoom;
import sendeverything.payload.response.RoomResponse;
import sendeverything.repository.DBRoomFileRepository;
import sendeverything.repository.RoomRepository;
import sendeverything.repository.UserRepository;
import sendeverything.repository.UserRoomRepository;

import javax.sql.rowset.serial.SerialBlob;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.sql.Blob;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class BulletinService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final UserRoomRepository userRoomRepository;

    @Autowired
    public BulletinService(RoomRepository roomRepository, UserRepository userRepository, UserRoomRepository userRoomRepository) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.userRoomRepository = userRoomRepository;
    }


    public String saveRoom(String title, String roomDescription , String roomPassword , MultipartFile roomImage , Optional<User> user, RoomType roomType ) throws Exception {
        LocalDateTime createTime = LocalDateTime.now();
        Blob image = convertToBlob(roomImage);

        Room room = new Room(generateRoomCode(8),title,roomDescription,roomPassword,image,roomType,createTime);
        user.ifPresent(room::setOwner);
        roomRepository.save(room);
        return room.getRoomCode();
    }

    public List<RoomResponse> getAllRooms(Principal principal) {
        Optional<User> optionalUser = principal != null ? userRepository.findByUsername(principal.getName()) : Optional.empty();
        List<Room> rooms = roomRepository.findAll();
        User currentUser = optionalUser.orElse(null);
        return rooms.stream().map(room -> convertToRoomResponse(room, currentUser)).collect(Collectors.toList());
    }

    private RoomResponse convertToRoomResponse(Room room, User currentUser) {
        RoomResponse roomResponse = new RoomResponse(); // 假设您已有一个构造RoomResponse的方法
        roomResponse.setRoomCode(room.getRoomCode());
        roomResponse.setTitle(room.getTitle());
        roomResponse.setDescription(room.getDescription());
        roomResponse.setRoomType(room.getRoomType());
        roomResponse.setCreateTime(room.getCreateTime());
        // 设置RoomResponse的其他属性...

        boolean isOwner = room.getOwner() != null && currentUser != null && room.getOwner().equals(currentUser);
        roomResponse.setIsOwner(isOwner);

        return roomResponse;
    }


    private String blobToBase64String(Blob blob) {
        if (blob == null) {
            return null;
        }
        try {
            InputStream inputStream = blob.getBinaryStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            byte[] blobBytes = outputStream.toByteArray();
            return Base64.getEncoder().encodeToString(blobBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert BLOB to string", e);
        }
    }





    public Blob convertToBlob(MultipartFile file) throws Exception {
        byte[] bytes = file.getBytes();
        Blob blob = new SerialBlob(bytes);
        return blob;
    }


    public String generateRoomCode(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder roomCode = new StringBuilder(length);
        Random random = new Random();

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            roomCode.append(characters.charAt(index));
        }
        if(roomRepository.existsByRoomCode(roomCode.toString())) {
            return generateRoomCode(length);
        }

        return roomCode.toString();
    }
    public String hashRoomCode(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not found.", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    private RoomResponse buildRoomResponse(Room room) {
        if (room == null) {
            return null;
        }
        RoomResponse response = new RoomResponse();
        response.setRoomCode(room.getRoomCode());
        response.setTitle(room.getTitle());
        response.setDescription(room.getDescription());
        response.setRoomType(room.getRoomType());
        response.setCreateTime(room.getCreateTime());
        response.setImage(blobToBase64String(room.getImage()));
        return response;
    }

    public RoomResponse findByRoomCode(String roomCode) {
        Room room = roomRepository.findByRoomCode(roomCode);
        System.out.println(room);
        return buildRoomResponse(room);  // 假設在這個方法中包含 dbRoomFiles
    }

    public RoomResponse accessRoom(String roomCode, String password) {
        Room room = roomRepository.findByRoomCodeAndPassword(roomCode, password);
        if (room == null) {
            throw new RoomNotFoundException("Room with the password is Incorrect.");
        }
        return buildRoomResponse(room);  // 假設在這個方法中不包含 dbRoomFiles
    }

    public Room findByRoomCode1(String roomCode) {
        Room room = roomRepository.findByRoomCode(roomCode);
        System.out.println(room);
        return room;  // 假設在這個方法中包含 dbRoomFiles
    }


    public void joinRoom(User user, Room room) {
        UserRoom userRoom = new UserRoom();
        userRoom.setUser(user);
        userRoom.setRoom(room);
        userRoom.setJoinedAt(LocalDateTime.now());
        userRoomRepository.save(userRoom);
    }

    public boolean hasUserJoinedRoom(User user, Room room) {
        return userRoomRepository.findByUserAndRoom(user, room) != null;
    }

}
