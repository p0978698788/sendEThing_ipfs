package sendeverything.security.jwt;

import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import sendeverything.security.services.UserDetailsImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtils {
  private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

  @Value("${bezkoder.app.jwtSecret}")
  private String jwtSecret;

  @Value("${bezkoder.app.jwtExpirationMs}")
  private int jwtExpirationMs;

  @Value("${bezkoder.app.jwtCookieName}")
  private String jwtCookie;
  @Value("jwt")
  private String jwtCookie_Google;

  public String readCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if ("jwt".equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }

  public String getJwtFromCookies(HttpServletRequest request) {
    Cookie cookie = WebUtils.getCookie(request, jwtCookie);
    if (cookie != null) {
      return cookie.getValue();
    } else {
      return null;
    }
  }
  public ResponseCookie generateJwtCookie(String username) { // 用於signing時，設定cookie
    String jwt = generateTokenFromUsername(username);
    // 設定cookie名稱與JWT，設定有效時間為一天，且只有在/api的請求下才會發送此cookie，並且不允許前端存取此cookie
    return ResponseCookie.from(jwtCookie, jwt).path("/api").maxAge(24 * 60 * 60).httpOnly(true).build();
  }

  public ResponseCookie generateJwtCookie(DefaultOidcUser oidcUser) { // 用於signing時，設定cookie
    String jwt = generateTokenFromUsername(oidcUser.getEmail().split("@")[0]);
    // 設定cookie名稱與JWT，設定有效時間為一天，且只有在/api的請求下才會發送此cookie，並且不允許前端存取此cookie
    return ResponseCookie.from(jwtCookie, jwt).path("/api").maxAge(24 * 60 * 60).httpOnly(true).build();
  }

  public ResponseCookie generateJwtCookie(UserDetailsImpl userPrincipal) { // 用於signing時，設定cookie
    String jwt = generateTokenFromUsername(userPrincipal.getUsername());
      // 設定cookie名稱與JWT，設定有效時間為一天，且只有在/api的請求下才會發送此cookie，並且不允許前端存取此cookie
    return ResponseCookie.from(jwtCookie, jwt).path("/api").maxAge(24 * 60 * 60).httpOnly(true).build();
  }

  public ResponseCookie getCleanJwtCookie() {// 用於sign-out時，清除cookie
      return ResponseCookie.from(jwtCookie, null).path("/api").build();
  }

  public List<ResponseCookie> getCleanJwtCookies() { // 用于 sign-out 时，清除 cookies
    List<ResponseCookie> cookies = new ArrayList<>();

    // 删除第一个 cookie
    ResponseCookie cookie1 = ResponseCookie.from("jwtCookie", null)
            .path("/api")
            .build();
    cookies.add(cookie1);

    // 删除第二个 cookie
    ResponseCookie cookie2 = ResponseCookie.from("jwtCookie_Goole", null)
            .path("/api")
            .build();
    cookies.add(cookie2);

    // 可以继续添加更多 cookies 的删除逻辑

    return cookies;
  }

  public String getUserNameFromJwtToken(String token) {
    return Jwts.parserBuilder().setSigningKey(key()).build()
        .parseClaimsJws(token).getBody().getSubject();
  }
  
  private Key key() {
    return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
  }

  public boolean validateJwtToken(String authToken) {
    try {
      Jwts.parserBuilder().setSigningKey(key()).build().parse(authToken);
      return true;
    } catch (MalformedJwtException e) {
      logger.error("Invalid JWT token: {}", e.getMessage());
    } catch (ExpiredJwtException e) {
      logger.error("JWT token is expired: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      logger.error("JWT token is unsupported: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      logger.error("JWT claims string is empty: {}", e.getMessage());
    }

    return false;
  }
  
  public String generateTokenFromUsername(String username) {   
    return Jwts.builder()
              .setSubject(username)
              .setIssuedAt(new Date())
              .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
              .signWith(key(), SignatureAlgorithm.HS256)
              .compact();
  }
}
