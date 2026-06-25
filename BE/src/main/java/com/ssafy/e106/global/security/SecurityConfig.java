package com.ssafy.e106.global.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.e106.global.exception.ErrorCode;
import com.ssafy.e106.global.exception.ErrorResponse;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final ObjectMapper objectMapper;

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/api/v1/auth/logout",
                "/api/v1/checkins/**",
                "/api/v1/merchant-mappings/**",
                "/api/v1/notifications/**",
                "/api/v1/promotions/**",
                "/api/v1/services/**",
                "/api/v1/finance/**",
                "/api/v1/subscriptions/**",
                "/api/v1/users/me",
                "/api/v1/users/me/**")
            .authenticated()
            .requestMatchers(
                "/",
                "/actuator/health",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/api/v1/health",
                "/api/v1/dev/**",
                "/api/v1/auth/login/**",
                "/api/v1/auth/refresh",
                "/api/v1/payment-history/**")
            .permitAll()
            .anyRequest()
            .permitAll())
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> {
            })
            .authenticationEntryPoint((request, response, exception) -> {
              response.setStatus(HttpStatus.UNAUTHORIZED.value());
              response.setContentType(MediaType.APPLICATION_JSON_VALUE);
              response.setCharacterEncoding("UTF-8");
              ErrorResponse body = ErrorResponse.of(ErrorCode.UNAUTHORIZED);
              objectMapper.writeValue(response.getWriter(), body);
            })
            .accessDeniedHandler((request, response, exception) -> {
              response.setStatus(HttpStatus.FORBIDDEN.value());
              response.setContentType(MediaType.APPLICATION_JSON_VALUE);
              response.setCharacterEncoding("UTF-8");
              ErrorResponse body = ErrorResponse.of(ErrorCode.FORBIDDEN);
              objectMapper.writeValue(response.getWriter(), body);
            }))
        .build();
  }
}
