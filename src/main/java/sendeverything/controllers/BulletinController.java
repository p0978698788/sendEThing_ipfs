package sendeverything.controllers;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sendeverything.models.User;
import sendeverything.models.room.Room;
import sendeverything.models.room.RoomType;
import sendeverything.payload.request.RoomRequest;
import sendeverything.payload.response.RoomCodeResponse;
import sendeverything.payload.response.RoomResponse;
import sendeverything.repository.UserRepository;
import sendeverything.service.room.BulletinService;
import software.amazon.awssdk.services.s3.model.MultipartUpload;

import java.security.Principal;
import java.sql.Blob;
import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = {"http://localhost", "http://localhost:8081, http://localhost:8080"}, allowCredentials = "true")
@RestController
@RequestMapping("/api/auth")
public class BulletinController {
    @Autowired
    private  BulletinService bulletinService;
    @Autowired
    private UserRepository userRepository;


    @PostMapping("/createRoom")
    public RoomCodeResponse createRoom(@RequestParam String title,
                                       @RequestParam RoomType roomType,
                                       @RequestParam String roomDescription,
                                       @RequestParam String roomPassword,
                                       @RequestParam MultipartFile roomImage,
                                       Principal principal) throws Exception {
        Optional<User> optionalUser = principal != null ? userRepository.findByUsername(principal.getName()) : Optional.empty();

        String roomCode =bulletinService.saveRoom(title,roomDescription,roomPassword,roomImage,optionalUser, roomType);



        return new RoomCodeResponse(roomCode);
    }
    @GetMapping("/getAllRooms")
    public List<RoomResponse> getAllRooms() {
        return bulletinService.getAllRooms();
    }
    @PostMapping("/accessRoom")
    public ResponseEntity<?> accessRoom(@RequestBody RoomRequest RoomRequest, HttpServletResponse response) {
        String roomCode = RoomRequest.getRoomCode();
        String password = RoomRequest.getPassword();
        RoomResponse roomResponse = bulletinService.accessRoom(roomCode, password);
        if (roomResponse != null ) {
            // 登入成功，設置 cookie
            String roomCookie= bulletinService.hashRoomCode(roomCode);
            Cookie cookie = new Cookie("AccessRoom", roomCookie);
            cookie.setHttpOnly(true); // 使 cookie 為 HTTP Only，提高安全性
            cookie.setPath("/"); // 設置 cookie 的路徑，如果需要限制為特定路徑，可以進行調整

            cookie.setMaxAge(60 * 60* 24); // 設置 cookie 的有效期，例如這裡是一個小時

            response.addCookie(cookie);
            System.out.println("cookie: "+cookie.getValue());
            return ResponseEntity.ok("Access Room Success"+roomCode);
        } else {
            // 登入失敗處理，例如返回一個錯誤響應
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Incorrect room code or password");
        }}
    @GetMapping("/verifyCookie")
    public ResponseEntity<String> checkCookie(HttpServletRequest request,@CookieValue("AccessRoom") String cookieValue){
        System.out.println("asdasd: ");
        if (cookieValue != null && !cookieValue.isEmpty()) {
            System.out.println("cookie: " + cookieValue);
            return ResponseEntity.ok("Authentication passed");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: No valid cookie found");
        }
    }

    @PostMapping("/showRoomContent")
    public ResponseEntity<?> showRoomContent(@RequestBody RoomRequest RoomRequest) {
        String roomCode = RoomRequest.getRoomCode();

        RoomResponse roomResponse = bulletinService.findByRoomCode(roomCode);

        return ResponseEntity.ok(roomResponse);
    }



}
