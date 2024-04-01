package sendeverything.security;


import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;


@Configuration
public class ResourcesConfig implements WebMvcConfigurer {

//    private static final ModelAndView FORBIDDEN_VIEW = new ModelAndView("redirect:/#/403");
//
//    private static final Map<HttpStatus.Series, String> SERIES_VIEWS;
//
//    static {
//        Map<HttpStatus.Series, String> views = new EnumMap<>(HttpStatus.Series.class);
//        views.put(HttpStatus.Series.CLIENT_ERROR, "/#/404");
//        views.put(HttpStatus.Series.SERVER_ERROR, "/#/500");
//        SERIES_VIEWS = Collections.unmodifiableMap(views);
//    }
//
//    @Bean
//    public ErrorViewResolver customErrorViewResolver() {
//        return (request, status, model) -> {
//            if (status.value() == HttpStatus.FORBIDDEN.value()) {
//                return FORBIDDEN_VIEW;
//            }
//            String view = SERIES_VIEWS.getOrDefault(status.series(), SERIES_VIEWS.get(HttpStatus.Series.SERVER_ERROR));
//            return new ModelAndView("redirect:" + view);
//        };
//    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/")
        .addResourceLocations("classpath:/static/index.html");
    }
}
