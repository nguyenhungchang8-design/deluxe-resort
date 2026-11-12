package com.deluxe.feature.feature2_booking.controller;

import com.deluxe.entity.User;
import com.deluxe.enums.BookingStatus;
import com.deluxe.feature.feature2_booking.dto.BookingDto;
import com.deluxe.feature.feature2_booking.dto.BookingRequest;
import com.deluxe.feature.feature2_booking.dto.CustomerOptionDto;
import com.deluxe.feature.feature2_booking.service.BookingService;
import com.deluxe.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * API Đặt phòng (Bookings)
 *
 * -- Dành cho Customer / Receptionist / Admin --
 * POST /api/bookings                        – Tạo đặt phòng (khách tự đặt hoặc lễ tân đặt hộ)
 * GET  /api/bookings/my                     – Xem booking của chính mình
 * GET  /api/bookings/{id}                   – Xem chi tiết booking (staff xem tất cả, khách xem của mình)
 * PUT  /api/bookings/{id}/cancel            – Khách/Lễ tân hủy booking
 *
 * -- Dành cho ADMIN/RECEPTIONIST --
 * GET  /api/bookings                        – Xem tất cả booking (có thể lọc ?status=)
 * GET  /api/bookings/customers              – Danh sách khách hàng để tạo booking hộ
 * GET  /api/bookings/status/{status}        – Lọc theo trạng thái
 * GET  /api/bookings/today/check-in         – Danh sách nhận phòng hôm nay
 * GET  /api/bookings/today/check-out        – Danh sách trả phòng hôm nay
 * PUT  /api/bookings/{id}/confirm           – Xác nhận booking
 * PUT  /api/bookings/{id}/check-in          – Nhận phòng
 * PUT  /api/bookings/{id}/check-out         – Trả phòng
 */
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    public BookingController(BookingService bookingService, UserRepository userRepository) {
        this.bookingService = bookingService;
        this.userRepository = userRepository;
    }

    // ============================================================
    //  Dành cho ADMIN / RECEPTIONIST
    // ============================================================

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ResponseEntity<List<BookingDto>> getAll(@RequestParam(required = false) BookingStatus status) {
        if (status != null) {
            return ResponseEntity.ok(bookingService.getByStatus(status));
        }
        return ResponseEntity.ok(bookingService.getAll());
    }

    @GetMapping("/customers")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ResponseEntity<List<CustomerOptionDto>> customers() {
        return ResponseEntity.ok(bookingService.getCustomerOptions());
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ResponseEntity<List<BookingDto>> getByStatus(@PathVariable BookingStatus status) {
        return ResponseEntity.ok(bookingService.getByStatus(status));
    }

    @GetMapping("/today/check-in")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ResponseEntity<List<BookingDto>> todayCheckIn() {
        return ResponseEntity.ok(bookingService.getTodayCheckIn());
    }

    @GetMapping("/today/check-out")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ResponseEntity<List<BookingDto>> todayCheckOut() {
        return ResponseEntity.ok(bookingService.getTodayCheckOut());
    }

    /** Lễ tân xác nhận booking: PENDING -> CONFIRMED */
    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ResponseEntity<BookingDto> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.confirm(id));
    }

    /** Lễ tân nhận phòng: CONFIRMED -> CHECKED_IN, Room -> OCCUPIED */
    @PutMapping("/{id}/check-in")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ResponseEntity<BookingDto> checkIn(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.checkIn(id));
    }

    /** Lễ tân trả phòng: CHECKED_IN -> CHECKED_OUT, Room -> CLEANING */
    @PutMapping("/{id}/check-out")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ResponseEntity<BookingDto> checkOut(@PathVariable Long id) {
        return ResponseEntity.ok(bookingService.checkOut(id));
    }

    // ============================================================
    //  Dành cho CUSTOMER / RECEPTIONIST / ADMIN
    // ============================================================

    /** Tạo đặt phòng: Customer đặt cho chính mình; Staff có thể truyền customerId đặt hộ khách */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','CUSTOMER')")
    public ResponseEntity<BookingDto> create(@Valid @RequestBody BookingRequest req,
                                             Authentication auth) {
        Long requesterId = resolveUserId(auth);
        Long targetCustomerId = (isStaff(auth) && req.getCustomerId() != null)
                ? req.getCustomerId()
                : requesterId;
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.create(targetCustomerId, req));
    }

    /** Khách xem booking của chính mình */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','CUSTOMER')")
    public ResponseEntity<List<BookingDto>> myBookings(Authentication auth) {
        Long customerId = resolveUserId(auth);
        return ResponseEntity.ok(bookingService.getByCustomer(customerId));
    }

    /** Xem chi tiết booking (Staff xem bất kỳ, Customer chỉ xem booking của mình) */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','CUSTOMER')")
    public ResponseEntity<BookingDto> getById(@PathVariable Long id, Authentication auth) {
        Long requesterId = resolveUserId(auth);
        return ResponseEntity.ok(bookingService.getByIdForUser(id, requesterId, isStaff(auth)));
    }

    /** Hủy booking: khách chỉ hủy booking của mình; staff hủy bất kỳ */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST','CUSTOMER')")
    public ResponseEntity<BookingDto> cancel(@PathVariable Long id, Authentication auth) {
        Long requesterId = resolveUserId(auth);
        return ResponseEntity.ok(bookingService.cancel(id, requesterId, isStaff(auth)));
    }

    // ---- Helper ----
    private boolean isStaff(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") ||
                               a.getAuthority().equals("ROLE_RECEPTIONIST"));
    }

    private Long resolveUserId(Authentication auth) {
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chua dang nhap");
        }
        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chua dang nhap"));
        return user.getId();
    }
}
