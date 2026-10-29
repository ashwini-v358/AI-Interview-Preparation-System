package com.smartinterview.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration for Cross-Origin Resource Sharing (CORS).
 *
 * Concepts introduced:
 * - CORS (Cross-Origin Resource Sharing): A security mechanism implemented by browsers
 *   that prevents a web app at one origin (e.g. http://localhost:5173 - React) from accessing
 *   resources at another origin (e.g. http://localhost:8080 - Spring Boot) unless the server
 *   explicitly permits it.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
