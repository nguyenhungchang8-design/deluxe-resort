package com.deluxe.config;

import com.deluxe.entity.User;
import com.deluxe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
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
                // 1. Tai nguyen tinh (CSS, JS, Fonts, Images)
                .requestMatchers("/css/**", "/js/**", "/images/**", "/fonts/**", "/scss/**").permitAll()
                .requestMatchers("/favicon.ico", "/error").permitAll()

                // 2. Cac trang web ngoai portal: cong khai danh cho Khach vang lai (Guest)
                .requestMatchers(
                    "/", "/index.html",
                    "/about.html",
                    "/rooms.html", "/rooms-single.html",
                    "/restaurant.html"
                ).permitAll()

                // 3. Trang Dang nhap & Dang ky (portal/auth) va API auth
                .requestMatchers("/portal/auth/**").permitAll()
                .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/logout").permitAll()
                .requestMatchers("/api/menu", "/api/menu/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                // 4. Bao ve cac thu muc chuc nang sau trong portal/ va api tuong ung theo Role
                .requestMatchers("/portal/admin/**", "/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/portal/reception/**", "/api/reception/**").hasAnyRole("ADMIN", "RECEPTIONIST")
                .requestMatchers("/portal/housekeeping/**", "/api/housekeeping/**").hasAnyRole("ADMIN", "HOUSEKEEPING", "RECEPTIONIST")
                .requestMatchers("/portal/accountant/**", "/api/accounting/**").hasAnyRole("ADMIN", "ACCOUNTANT")
                .requestMatchers("/portal/waiter/**", "/api/waiter/**").hasAnyRole("ADMIN", "WAITER")
                .requestMatchers("/portal/customer/**").hasAnyRole("ADMIN", "CUSTOMER")

                // 5. Tat ca cac duong dan con lai trong portal/** va api/** bat buoc phai dang nhap
                .requestMatchers("/portal/**", "/api/**").authenticated()

                // 6. Cac request khac
                .anyRequest().permitAll()
            )
            .exceptionHandling(e -> e
                .authenticationEntryPoint((request, response, authException) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/portal/auth/login.html");
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.sendError(HttpStatus.FORBIDDEN.value(), "Forbidden");
                    } else {
                        response.sendRedirect(request.getContextPath() + "/portal/auth/access-denied.html");
                    }
                })
            )
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
}


