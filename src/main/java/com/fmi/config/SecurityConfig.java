package com.fmi.config;

import com.fmi.security.CustomUserDetailsService;
import com.fmi.security.JwtAuthenticationFilter;
import com.fmi.security.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtTokenProvider tokenProvider, CustomUserDetailsService userDetailsService) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/actuator/health", "/actuator/info", "/ws/**", "/error", "/health", "/api/health").permitAll()
                        .requestMatchers("/auth/login", "/auth/signup", "/auth/refresh", "/auth/logout",
                                "/auth/check-email", "/auth/check-nickname", "/auth/reset/**", "/s3/**", "/chat-test.html").permitAll()
                        .requestMatchers("/auth/kakao/**", "/auth/email/**", "/auth/phone/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        // 공지사항, FAQ, 공개 문의 - 공개 API (경로 개편)
                        .requestMatchers("/notice/**", "/faq/**", "/inquiry/public/**").permitAll()
                        // 관리자 전용 API 보호
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .httpBasic(h -> h.disable());

        http.addFilterBefore(new JwtAuthenticationFilter(tokenProvider, userDetailsService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}



