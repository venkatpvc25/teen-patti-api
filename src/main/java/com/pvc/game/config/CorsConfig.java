package com.pvc.game.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        // 🔐 Allow only your frontend domains
        config.setAllowedOrigins(List.of(
                "http://localhost:3000", // React dev
                "http://localhost:5173", // Vite
                "https://yourdomain.com" // Production
        ));

        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "PATCH"));

        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type"));

        config.setAllowCredentials(true);

        config.setExposedHeaders(List.of(
                "Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
