package com.deluxe.feature.feature1_auth;

import com.deluxe.entity.User;
import com.deluxe.feature.feature1_auth.dto.ChangePasswordRequest;
import com.deluxe.feature.feature1_auth.dto.CreateStaffRequest;
import com.deluxe.feature.feature1_auth.dto.UpdateProfileRequest;
import com.deluxe.feature.feature1_auth.dto.UserResponseDto;
import com.deluxe.feature.feature1_auth.service.UserService;
import com.deluxe.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test toàn diện cho UserService (Feature 1: Authentication & User Management)
 * Kiểm thử chi tiết các Use Cases:
 * - UC04: Update Profile
 * - UC05: Change Password
 * - UC06: Admin Create Staff
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .username("john_doe")
                .password("encoded_pass")
                .fullName("John Doe")
                .email("john@example.com")
                .role("ROLE_CUSTOMER")
                .build();
    }

    // ============================================================
    // Test Case: UC04 - Update Profile
    // ============================================================

    @Test
    @DisplayName("UC04: Cập nhật hồ sơ thành công khi dữ liệu hợp lệ")
    void testUpdateProfile_Success() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("John Updated")
                .email("john_updated@example.com")
                .build();

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByEmail("john_updated@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDto result = userService.updateProfile("john_doe", request);

        assertNotNull(result);
        assertEquals("John Updated", result.getFullName());
        assertEquals("john_updated@example.com", result.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("UC04: Cập nhật hồ sơ thất bại khi email mới bị trùng với người khác")
    void testUpdateProfile_EmailDuplicate() {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("John Updated")
                .email("duplicate@example.com")
                .build();

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.updateProfile("john_doe", request));

        assertTrue(ex.getMessage().contains("đã tồn tại"));
        verify(userRepository, never()).save(any());
    }

    // ============================================================
    // Test Case: UC05 - Change Password
    // ============================================================

    @Test
    @DisplayName("UC05: Đổi mật khẩu thành công khi đúng mật khẩu cũ")
    void testChangePassword_Success() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldPassword123")
                .newPassword("newPassword456")
                .build();

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("oldPassword123", "encoded_pass")).thenReturn(true);
        when(passwordEncoder.matches("newPassword456", "encoded_pass")).thenReturn(false);
        when(passwordEncoder.encode("newPassword456")).thenReturn("new_encoded_pass");

        assertDoesNotThrow(() -> userService.changePassword("john_doe", request));
        verify(userRepository).save(sampleUser);
        assertEquals("new_encoded_pass", sampleUser.getPassword());
    }

    @Test
    @DisplayName("UC05: Đổi mật khẩu thất bại khi nhập sai mật khẩu cũ")
    void testChangePassword_WrongCurrentPassword() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("wrongPassword")
                .newPassword("newPassword456")
                .build();

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrongPassword", "encoded_pass")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.changePassword("john_doe", request));

        assertTrue(ex.getMessage().contains("Mật khẩu hiện tại không chính xác"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("UC05: Đổi mật khẩu thất bại khi mật khẩu mới trùng mật khẩu cũ")
    void testChangePassword_SameAsOldPassword() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("oldPassword123")
                .newPassword("oldPassword123")
                .build();

        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("oldPassword123", "encoded_pass")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.changePassword("john_doe", request));

        assertTrue(ex.getMessage().contains("không được trùng"));
        verify(userRepository, never()).save(any());
    }

    // ============================================================
    // Test Case: UC06 - Admin Create Staff
    // ============================================================

    @Test
    @DisplayName("UC06: Admin tạo tài khoản nhân viên thành công")
    void testCreateStaff_Success() {
        CreateStaffRequest request = CreateStaffRequest.builder()
                .username("reception01")
                .password("staff123")
                .fullName("Nguyen Van A")
                .email("reception01@deluxe.com")
                .role("RECEPTIONIST")
                .build();

        when(userRepository.existsByUsername("reception01")).thenReturn(false);
        when(userRepository.existsByEmail("reception01@deluxe.com")).thenReturn(false);
        when(passwordEncoder.encode("staff123")).thenReturn("encoded_staff_pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDto created = userService.createStaff(request);

        assertNotNull(created);
        assertEquals("reception01", created.getUsername());
        assertEquals("ROLE_RECEPTIONIST", created.getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("UC06: Admin tạo nhân viên thất bại khi username đã tồn tại")
    void testCreateStaff_UsernameExists() {
        CreateStaffRequest request = CreateStaffRequest.builder()
                .username("existing_user")
                .password("pass123")
                .fullName("Existing User")
                .email("test@deluxe.com")
                .role("WAITER")
                .build();

        when(userRepository.existsByUsername("existing_user")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                userService.createStaff(request));

        assertTrue(ex.getMessage().contains("đã tồn tại"));
        verify(userRepository, never()).save(any());
    }
}
