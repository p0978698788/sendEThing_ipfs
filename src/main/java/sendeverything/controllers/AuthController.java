package sendeverything.controllers;

import sendeverything.security.services.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import sendeverything.payload.request.LoginRequest;
import sendeverything.payload.request.SignupRequest;
import sendeverything.payload.response.UserInfoResponse;
import sendeverything.payload.response.MessageResponse;
import sendeverything.repository.UserRepository;
import sendeverything.security.jwt.JwtUtils;

//for Angular Client (withCredentials)
@CrossOrigin(origins = "http://localhost:8080, http://localhost:8081, http://localhost:8080", maxAge = 3600, allowCredentials="true")
//@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
  private final UserRepository userRepository;
  private final AuthenticationService service;
  private final JwtUtils jwtUtils;


//  @GetMapping("/oauth2/redirect")
//  public ResponseEntity<?> oauth2Redirect(@Valid  HttpServletRequest request ) {
//    Cookie[] cookies = request.getCookies();
//    if (cookies != null) {
//      for (Cookie cookie : cookies) {
//        if ("jwt".equals(cookie.getName())) {
//          String jwt = cookie.getValue();
////          System.out.println("jwt:" + jwt);
//          String username = jwtUtils.getUserNameFromJwtToken(jwt);
//          UserInfoResponse userInfoResponse = service.processOAuthPostLogin(username);
//          ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(username);
//          System.out.println("username : " + username + " Login Success!");
//          return ResponseEntity.ok()
//                  .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
//                  .body(userInfoResponse);
//        }
//      }
//    }return ResponseEntity.badRequest().body(new MessageResponse("Error: Login failed!"));
//  }

  @GetMapping("/oauth2/redirect")
  public ResponseEntity<?> oauth2Redirect(@CookieValue(name = "bezkoder", required = false) String jwt) {
    if (jwt != null) {
      String username = jwtUtils.getUserNameFromJwtToken(jwt);
      UserInfoResponse userInfoResponse = service.processOAuthPostLogin(username);
      ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(username);
      System.out.println("username : " + username + " Login Success!");
      return ResponseEntity.ok()
              .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
              .body(userInfoResponse);
    } else {
      return ResponseEntity.badRequest().body(new MessageResponse("Error: Login failed!"));
    }
  }







  @PostMapping("/signin")
  public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

    UserInfoResponse userInfoResponse = service.authenticate(loginRequest);

    if(userInfoResponse != null) {

        ResponseCookie jwtCookie = userInfoResponse.getJwtCookie();
        System.out.println("username : " + userInfoResponse.getUsername() + " Login Success!");

      return ResponseEntity.ok()
              .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .body(userInfoResponse);
    } else {
      return ResponseEntity.badRequest().body(new MessageResponse("Error: Login failed!"));
    }

  }

  @PostMapping("/signup")
  public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
    if (userRepository.existsByUsername(signUpRequest.getUsername())) {
      return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
    }

    if (userRepository.existsByEmail(signUpRequest.getEmail())) {
      return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
    }// 以上兩個if判斷，若有重複的username或email，則回傳錯誤訊息

    // Create new user's account
    return ResponseEntity.ok(service.register(signUpRequest));
  }

  @PostMapping("/signout")
  public ResponseEntity<?> logoutUser() {
    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, service.logoutUser().toString())
        .body(new MessageResponse("You've been signed out!"));
  }

  @GetMapping("/checkAuth")
  public ResponseEntity<?> checkUser(HttpServletRequest request) {

    return service.checkAuth(request);
  }


//  @GetMapping("/a")
//  public String generateContent()throws Exception {
//    String input = "inputText";
////        FileProcessor fileProcessor = new FileProcessor();
////        String output = fileProcessor.generateContent(input);
//    System.out.println("inputText: "+input );
//    return input;
//  }
}
