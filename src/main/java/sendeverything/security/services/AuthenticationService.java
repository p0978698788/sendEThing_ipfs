package sendeverything.security.services;

import sendeverything.models.ERole;
import sendeverything.models.Provider;
import sendeverything.models.Role;
import sendeverything.models.User;
import sendeverything.payload.request.LoginRequest;
import sendeverything.payload.request.SignupRequest;
import sendeverything.payload.response.MessageResponse;
import sendeverything.payload.response.UserInfoResponse;
import sendeverything.security.jwt.AuthTokenFilter;
import sendeverything.security.jwt.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import sendeverything.repository.RoleRepository;
import sendeverything.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor// 會自動生成一個構造器，用於對所有final和@NonNull字段進行初始化，會有依賴注入的效果，不需要顯式地使用@Autowired注入
public class AuthenticationService {

    private final PasswordEncoder encoder;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private static final Logger logger = LoggerFactory.getLogger(AuthTokenFilter.class);

    public UserInfoResponse processOAuthPostLogin( String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User Not Found with username: " + username));
        String jwtToken = jwtUtils.generateTokenFromUsername(username);


        return UserInfoResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .accessToken(jwtToken)
                .build();
    }

    public Boolean googleUser_inLocal(String email) {
        return userRepository.findProviderByEmail(email);
    }

    public Optional<User> processGoogleUserInLocal(String email) {
        return userRepository.findByEmail(email);
    }




    public void saveGoogleUser(Authentication authentication, String username, String email) {

        User user = userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(username);
                    newUser.setEmail(email);
                    newUser.setPassword("");
                    newUser.setProvider(Provider.GOOGLE);
                    newUser.setRoles(Set.of(roleRepository.findByName(ERole.ROLE_USER).get()));
                    System.out.println("save user");
                    return userRepository.save(newUser);
                });
    }



    public UserInfoResponse authenticate(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager // AuthenticationManager是一個介面，它只有一個方法authenticate，用來驗證傳入的Authentication物件是否有效。
                .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())); // UsernamePasswordAuthenticationToken是Authentication的實作類別，用來儲存使用者的帳號密碼。
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);// 產生cookie

        String jwtToken = jwtUtils.generateTokenFromUsername(userDetails.getUsername());



        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return UserInfoResponse.builder()
                .id(userDetails.getId())
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .roles(roles)
                .accessToken(jwtToken)
                .jwtCookie(jwtCookie)
                .build();
    }

    public MessageResponse register(SignupRequest signUpRequest) {

        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()),
                Provider.LOCAL);

        Set<String> strRoles = signUpRequest.getRole();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(adminRole);

                        break;
                    case "mod":
                        Role modRole = roleRepository.findByName(ERole.ROLE_MODERATOR)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(modRole);

                        break;
                    default:
                        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        userRepository.save(user);
        return new MessageResponse("User registered successfully!");
    }

    public ResponseCookie logoutUser() {
        return jwtUtils.getCleanJwtCookie();
    }


    public ResponseEntity<?> checkAuth(HttpServletRequest request) {
        String jwt = jwtUtils.getJwtFromCookies(request);
        boolean isValid = jwtUtils.validateJwtToken(jwt);
        logger.info("isValid: {}", isValid);
        if(isValid) {
            return ResponseEntity.ok().body(new MessageResponse("You've been signed in!"));
        } else{
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Login failed!"));
        }
    }

}