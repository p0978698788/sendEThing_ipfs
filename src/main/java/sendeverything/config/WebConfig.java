package sendeverything.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/auth/downloadFileByCode/**")  // 或者更具体的路径
                .allowedOrigins("http://localhost:8081")
                .exposedHeaders(HttpHeaders.CONTENT_DISPOSITION);
    }

}