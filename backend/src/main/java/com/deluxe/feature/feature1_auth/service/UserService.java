package com.deluxe.feature.feature1_auth.service;

import com.deluxe.entity.User;
import com.deluxe.feature.feature1_auth.dto.ChangePasswordRequest;
import com.deluxe.feature.feature1_auth.dto.CreateStaffRequest;
import com.deluxe.feature.feature1_auth.dto.UpdateProfileRequest;
import com.deluxe.feature.feature1_auth.dto.UserResponseDto;
import com.deluxe.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service xử lý các nghiệp vụ User:
 * - UC04: Cập nhật hồ sơ cá nhân
 * - UC05: Đổi mật khẩu
 * - UC06: Admin tạo và quản lý tài khoản nhân viên
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Lấy thông tin user hiện tại theo username
     */
    public UserResponseDto getCurrentUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng: " + username));
        return toDto(user);
    }

    /**
     * UC04: Cập nhật hồ sơ cá nhân
     */
    @Transactional
    public UserResponseDto updateProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng: " + username));

        // Kiểm tra nếu email thay đổi thì không trùng với user khác
        if (!user.getEmail().equalsIgnoreCase(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email " + request.getEmail() + " đã tồn tại trên hệ thống");
            }
        }

        user.setFullName(request.getFullName().trim());
        user.setEmail(request.getEmail().trim());

        User saved = userRepository.save(user);
        return toDto(saved);
    }

    /**
     * UC05: Đổi mật khẩu
     */
    @Transactional
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng: " + username));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu mới không được trùng với mật khẩu cũ");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * UC06: Admin tạo tài khoản nhân viên (Receptionist, Waiter, Housekeeping, Manager)
     */
    @Transactional
    public UserResponseDto createStaff(CreateStaffRequest request) {
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new IllegalArgumentException("Tên đăng nhập '" + request.getUsername() + "' đã tồn tại");
        }
        if (userRepository.existsByEmail(request.getEmail().trim())) {
            throw new IllegalArgumentException("Email '" + request.getEmail() + "' đã tồn tại trên hệ thống");
        }

        String role = request.getRole().trim();
        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role.toUpperCase();
        }

        User staff = User.builder()
                .username(request.getUsername().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .email(request.getEmail().trim())
                .role(role)
                .build();

        User saved = userRepository.save(staff);
        return toDto(saved);
    }

    /**
     * Danh sách tất cả tài khoản người dùng (Admin)
     */
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Helper chuyển đổi Entity sang DTO
     */
    private UserResponseDto toDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
