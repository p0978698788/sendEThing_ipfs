package sendeverything.security.jwt;

import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import sendeverything.exception.CustomJwtException;
import sendeverything.exception.JwtErrorType;
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
  @Value("${bezkoder.app.jwtExpirationRefreshMs}")
  private int jwtExpirationRefreshMs;

  @Value("${bezkoder.app.jwtCookieName}")
  private String jwtCookie;
  @Value("${bezkoder.app.jwtRefreshCookieName}")
  private String jwtCookieRefresh;
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
    return ResponseCookie.from(jwtCookie, jwt).path("/api").maxAge( 24*60 * 60).httpOnly(true).build();
  }

  public ResponseCookie createRefreshTokenCookie(String username) {
    String refreshToken = generateRefreshToken(username);
    // 创建一个HttpOnly的Cookie来存储Refresh Token
    return ResponseCookie.from(jwtCookieRefresh, refreshToken)  // 使用ResponseCookie.from创建新的Cookie，指定名称和值
            .path("/")                             // 设置Cookie的路径
            .httpOnly(true)                       // 设置为HttpOnly，增加安全性，防止客户端JavaScript访问
            .secure(true)                         // 设置为Secure，推荐只在HTTPS环境下使用
            .sameSite("Strict")                   // 设置SameSite属性，可以是Strict或Lax，增加对跨站请求伪造（CSRF）的保护
            .maxAge(48*60 * 60)             // 设置Cookie的最大存活时间，例如这里设置为两天
            .build();                             // 构建并返回配置好的ResponseCookie对象
  }

//  public ResponseCookie generateJwtCookie(DefaultOidcUser oidcUser) { // 用於signing時，設定cookie
//    String jwt = generateTokenFromUsername(oidcUser.getEmail().split("@")[0]);
//    // 設定cookie名稱與JWT，設定有效時間為一天，且只有在/api的請求下才會發送此cookie，並且不允許前端存取此cookie
//    return ResponseCookie.from(jwtCookie, jwt).path("/api").maxAge(24 * 60 * 60).httpOnly(true).build();
//  }

//  public ResponseCookie generateJwtCookie(UserDetailsImpl userPrincipal) { // 用於signing時，設定cookie
//    String jwt = generateTokenFromUsername(userPrincipal.getUsername());
//      // 設定cookie名稱與JWT，設定有效時間為一天，且只有在/api的請求下才會發送此cookie，並且不允許前端存取此cookie
//    return ResponseCookie.from(jwtCookie, jwt).path("/api").maxAge(24 * 60 * 60).httpOnly(true).build();
//  }

  public ResponseCookie getCleanJwtCookie() {// 用於sign-out時，清除cookie

    return ResponseCookie.from(jwtCookie, null).path("/api").build();
  }





  public String getUserNameFromJwtToken(String token) {
    return Jwts.parserBuilder().setSigningKey(key()).build()
        .parseClaimsJws(token).getBody().getSubject();
  }
  
  private Key key() {
    return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
  }

  public boolean validateJwtToken(String authToken) throws CustomJwtException {
    try {
      Jwts.parserBuilder().setSigningKey(key()).build().parse(authToken);
    } catch (MalformedJwtException e) {
      logger.error("Invalid JWT token: {}", e.getMessage());
      throw new CustomJwtException("Invalid JWT token", JwtErrorType.MALFORMED);
    } catch (ExpiredJwtException e) {
      logger.error("JWT token is expired: {}", e.getMessage());
      throw new CustomJwtException("JWT token is expired", JwtErrorType.EXPIRED);
    } catch (UnsupportedJwtException e) {
      logger.error("JWT token is unsupported: {}", e.getMessage());
      throw new CustomJwtException("JWT token is unsupported", JwtErrorType.UNSUPPORTED);
    } catch (IllegalArgumentException e) {
      logger.error("JWT claims string is empty: {}", e.getMessage());
      throw new CustomJwtException("JWT claims string is empty", JwtErrorType.ILLEGAL_ARGUMENT);
    }
    return true;
  }

  

  public String getRefreshTokenFromCookies(HttpServletRequest request) {
    // 从Cookie中获取Refresh Token的实现逻辑
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (jwtCookieRefresh.equals(cookie.getName())) {
          return cookie.getValue();
        }
      }
    }
    return null;
  }

  public String generateTokenFromUsername(String username) {
    return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
            .signWith(key(), SignatureAlgorithm.HS256)
            .compact();
  }


  public String generateRefreshToken(String username) {

      // 設定Refresh Token的有效期比Access Token更長

    return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(new Date((new Date()).getTime() + jwtExpirationRefreshMs))
            .signWith(key(), SignatureAlgorithm.HS256)
            .compact();
  }
  public Cookie createJwtCookie(String jwt) {
    // 创建一个HttpOnly的Cookie来存储JWT
    Cookie cookie = new Cookie("jwt", jwt);
    cookie.setHttpOnly(true);
    cookie.setPath("/");
    return cookie;
  }

}
