package com.anwar.cloudshareapi.config;

import com.anwar.cloudshareapi.security.ClerkJwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ClerkJwtAuthFilter clerkJwtAuthFilter;
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .cors(Customizer.withDefaults()) //Enables Cross-Origin Resource Sharing (if needed for frontend calls).
                .csrf(AbstractHttpConfigurer::disable) //CSRF disabled → suitable for REST APIs.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/webhooks/**").permitAll()   // This endpoint is public
                        .anyRequest().authenticated()                  // All others need authentication
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) //Server will not store session — required for JWT.
                )
                .addFilterBefore(clerkJwtAuthFilter, UsernamePasswordAuthenticationFilter.class);


        return httpSecurity.build();
    }

    public CorsFilter corsFilter(){
        return new CorsFilter(corsConfigurationSource());
    }

        private UrlBasedCorsConfigurationSource corsConfigurationSource(){
            CorsConfiguration config=new CorsConfiguration();
            config.setAllowedOrigins(List.of("*"));
            config.setAllowedOrigins(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
            config.setAllowedOrigins(List.of("Authorization","Content-Type"));
            config.setAllowCredentials(true);

            UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**",config);
            return source;
        }


}
