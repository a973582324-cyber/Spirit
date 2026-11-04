package com.admaterial.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 层配置
 * —— 跨域（CORS）支持 + 静态资源
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")                    // 课程 demo 允许所有来源
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }

    // 可扩展：文件上传目录映射为静态资源
    // @Override
    // public void addResourceHandlers(ResourceHandlerRegistry registry) {
    //     registry.addResourceHandler("/uploads/**")
    //             .addResourceLocations("file:" + uploadDir);
    // }
}
