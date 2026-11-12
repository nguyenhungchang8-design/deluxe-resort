package com.deluxe.feature.feature2_booking.controller;

import com.deluxe.enums.RoomStatus;
import com.deluxe.feature.feature2_booking.dto.RoomDto;
import com.deluxe.feature.feature2_booking.dto.RoomRequest;
import com.deluxe.feature.feature2_booking.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * API Phòng (Rooms)
 * GET  /api/rooms                            – Danh sách phòng (đã đăng nhập, lọc tùy chọn roomTypeId, status)
 * GET  /api/rooms/{id}                       – Chi tiết phòng (đã đăng nhập)
 * GET  /api/rooms/available?checkIn=&checkOut= – Tìm phòng trống theo ngày (đã đăng nhập)
 * GET  /api/rooms/status/{status}            – Lọc phòng theo trạng thái (ADMIN/RECEPTIONIST)
 * POST /api/rooms                            – Thêm phòng mới (ADMIN)
 * PUT  /api/rooms/{id}                       – Cập nhật phòng (ADMIN/RECEPTIONIST)
 * PUT  /api/rooms/{id}/status               – Chuyển trạng thái phòng (ADMIN/RECEPTIONIST)
 * DELETE /api/rooms/{id}                     – Xóa phòng (ADMIN)
 */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public ResponseEntity<List<RoomDto>> getAll(
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) RoomStatus status) {
        return ResponseEntity.ok(roomService.getFiltered(roomTypeId, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getById(id));
    }

    /** Tìm phòng còn trống trong khoảng ngày (chống trùng lịch) */
    @GetMapping("/available")
    public ResponseEntity<List<RoomDto>> getAvailable(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) Integer capacity) {
        return ResponseEntity.ok(roomService.findAvailableRooms(checkIn, checkOut, roomTypeId, capacity));
    }

    /** Lọc phòng theo trạng thái (ADMIN/RECEPTIONIST) */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ResponseEntity<List<RoomDto>> getByStatus(@PathVariable RoomStatus status) {
        return ResponseEntity.ok(roomService.getByStatus(status));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomDto> create(@Valid @RequestBody RoomRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ResponseEntity<RoomDto> update(@PathVariable Long id,
                                          @Valid @RequestBody RoomRequest req) {
        return ResponseEntity.ok(roomService.update(id, req));
    }

    /** Thay đổi trạng thái phòng: body { "status": "AVAILABLE" } */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','RECEPTIONIST')")
    public ResponseEntity<RoomDto> updateStatus(@PathVariable Long id,
                                                @RequestBody Map<String, String> body) {
        String raw = body != null ? body.get("status") : null;
        if (raw == null || raw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trang thai phong khong duoc de trong");
        }
        RoomStatus newStatus;
        try {
            newStatus = RoomStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trang thai phong khong hop le: " + raw);
        }
        return ResponseEntity.ok(roomService.updateStatus(id, newStatus));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roomService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
