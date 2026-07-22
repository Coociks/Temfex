package com.coociks.temfex.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Отключаем CSRF для упрощения работы с нашим фронтендом и API
            .csrf(csrf -> csrf.disable())
            
            // Делаем сессию stateless (без сохранения состояния), так как у нас пока нет полноценной авторизации
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Настраиваем правила доступа
            .authorizeHttpRequests(auth -> auth
                // Разрешаем доступ к главной странице, статике и всем нашим API без пароля
                .requestMatchers("/", "/index.html", "/s/**", "/api/**").permitAll()
                // Всё остальное (на будущее) требует авторизации
                .anyRequest().authenticated()
            );

        return http.build();
    }
}