package com.deluxe.config;

import com.deluxe.feature1_auth.model.User;
import com.deluxe.feature1_auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository repo) {
        return username -> repo.findByUsername(username)
                .map(u -> org.springframework.security.core.userdetails.User
                        .withUsername(u.getUsername())
                        .password(u.getPassword())
                        .authorities(new SimpleGrantedAuthority(u.getRole()))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Khong tim thay user"));
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(c -> c.configurationSource(corsSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/logout").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/reception/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                .requestMatchers("/api/housekeeping/**").hasAnyRole("ADMIN", "HOUSEKEEPING", "RECEPTIONIST")
                .requestMatchers("/api/accounting/**").hasAnyRole("ADMIN", "ACCOUNTANT")
                .requestMatchers("/api/waiter/**").hasAnyRole("ADMIN", "WAITER")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll())
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(allowedOrigins);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }

    /** Tao tai khoan mac dinh cho tung role (mat khau: admin123 cho admin, 123456 cho cac tai khoan con lai) */
    @Bean
    CommandLineRunner seedAccounts(UserRepository repo, PasswordEncoder encoder) {
        return args -> {
            seed(repo, encoder, "admin", "admin123", "Administrator", "ROLE_ADMIN");
            seed(repo, encoder, "customer", "123456", "Khach Hang", "ROLE_CUSTOMER");
            seed(repo, encoder, "reception", "123456", "Le Tan", "ROLE_RECEPTIONIST");
            seed(repo, encoder, "housekeeping", "123456", "Buong Phong", "ROLE_HOUSEKEEPING");
            seed(repo, encoder, "accountant", "123456", "Ke Toan", "ROLE_ACCOUNTANT");
            seed(repo, encoder, "waiter", "123456", "Phuc Vu", "ROLE_WAITER");
        };
    }

    private void seed(UserRepository repo, PasswordEncoder encoder, String username, String rawPassword,
                      String fullName, String role) {
        if (repo.existsByUsername(username)) return;
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@deluxe.com");
        u.setFullName(fullName);
        u.setPassword(encoder.encode(rawPassword));
        u.setRole(role);
        repo.save(u);
    }
}


