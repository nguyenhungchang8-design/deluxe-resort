package com.deluxe.feature.feature2_booking.controller;

import com.deluxe.feature.feature2_booking.dto.RoomTypeDto;
import com.deluxe.feature.feature2_booking.dto.RoomTypeRequest;
import com.deluxe.feature.feature2_booking.service.RoomTypeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API Hạng phòng (Room Types)
 * GET /api/room-types        – Tất cả người dùng đã đăng nhập
 * GET /api/room-types/{id}   – Tất cả người dùng đã đăng nhập
 * POST/PUT/DELETE            – Chỉ ADMIN
 */
@RestController
@RequestMapping("/api/room-types")
public class RoomTypeController {

    private final RoomTypeService roomTypeService;

    public RoomTypeController(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
    }

    @GetMapping
    public ResponseEntity<List<RoomTypeDto>> getAll() {
        return ResponseEntity.ok(roomTypeService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomTypeDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(roomTypeService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomTypeDto> create(@Valid @RequestBody RoomTypeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomTypeService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomTypeDto> update(@PathVariable Long id,
                                              @Valid @RequestBody RoomTypeRequest req) {
        return ResponseEntity.ok(roomTypeService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roomTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

