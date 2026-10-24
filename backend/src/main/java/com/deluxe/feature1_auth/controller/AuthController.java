package com.deluxe.feature1_auth.controller;

import com.deluxe.feature1_auth.model.User;
import com.deluxe.feature1_auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50) String username,
            @NotBlank @Email String email,
            @Size(max = 100) String fullName,
            @NotBlank @Size(min = 6, max = 100) String password) {}

    private final AuthenticationManager authManager;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final HttpSessionSecurityContextRepository contextRepo = new HttpSessionSecurityContextRepository();

    public AuthController(AuthenticationManager authManager, UserRepository users, PasswordEncoder encoder) {
        this.authManager = authManager;
        this.users = users;
        this.encoder = encoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req,
                                   HttpServletRequest request, HttpServletResponse response) {
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password()));
            SecurityContext ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(auth);
            SecurityContextHolder.setContext(ctx);
            contextRepo.saveContext(ctx, request, response);
            return ResponseEntity.ok(toDto(users.findByUsername(req.username()).orElseThrow()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Sai tai khoan hoac mat khau"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (users.existsByUsername(req.username()))
            return ResponseEntity.badRequest().body(Map.of("message", "Ten dang nhap da ton tai"));
        if (users.existsByEmail(req.email()))
            return ResponseEntity.badRequest().body(Map.of("message", "Email da duoc su dung"));
        User u = new User();
        u.setUsername(req.username());
        u.setEmail(req.email());
        u.setFullName(req.fullName());
        u.setPassword(encoder.encode(req.password()));
        users.save(u);
        return ResponseEntity.ok(toDto(u));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        return users.findByUsername(auth.getName())
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(toDto(u)))
                .orElse(ResponseEntity.status(401).build());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response, Authentication auth) {
        new SecurityContextLogoutHandler().logout(request, response, auth);
        return ResponseEntity.ok(Map.of("message", "Da dang xuat"));
    }

    private Map<String, Object> toDto(User u) {
        return Map.of("id", u.getId(), "username", u.getUsername(), "email", u.getEmail(),
                "fullName", u.getFullName() == null ? "" : u.getFullName(), "role", u.getRole());
    }
}



