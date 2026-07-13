
package com.pvc.game.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.pvc.game.security.JwtAuthenticationEntryPoint;
import com.pvc.game.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtFilter;
        private final JwtAuthenticationEntryPoint entryPoint;

        @Value("${app.docs.public-enabled:false}")
        private boolean docsPublicEnabled;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                return http
                                .cors(cors -> {
                                })
                                .csrf(csrf -> csrf.disable())
                                .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint))
                                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> {
                                        auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                                        auth.requestMatchers(
                                                        "/auth/**",
                                                        "/ws/**",
                                                        "/actuator/health")
                                                        .permitAll();
                                        if (docsPublicEnabled) {
                                                auth.requestMatchers(
                                                                "/v3/api-docs/**",
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html")
                                                                .permitAll();
                                        }
                                        auth.anyRequest().authenticated();
                                })
                                .addFilterBefore(jwtFilter,
                                                UsernamePasswordAuthenticationFilter.class)
                                .build();
        }
}
