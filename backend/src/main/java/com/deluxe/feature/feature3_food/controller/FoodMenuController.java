package com.deluxe.feature.feature3_food.controller;

import com.deluxe.entity.MenuItem;
import com.deluxe.feature.feature3_food.service.FoodOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller quản lý thực đơn món ăn (Menu) - UC15
 * Người thực hiện: Nguyễn Lâm Phúc (Member 3)
 */
@RestController
@RequestMapping("/api/menu")
public class FoodMenuController {

    private final FoodOrderService foodOrderService;

    public FoodMenuController(FoodOrderService foodOrderService) {
        this.foodOrderService = foodOrderService;
    }

    /**
     * Khách hàng & Phục vụ xem thực đơn đang bán (UC15)
     * URL: GET /api/menu
     */
    @GetMapping
    public ResponseEntity<List<MenuItem>> getAvailableMenu() {
        return ResponseEntity.ok(foodOrderService.getAvailableMenu());
    }

    /**
     * Xem tất cả các món ăn (bao gồm món đã hết) dành cho Waiter/Admin
     * URL: GET /api/menu/all
     */
    @GetMapping("/all")
    public ResponseEntity<List<MenuItem>> getAllMenu() {
        return ResponseEntity.ok(foodOrderService.getAllMenu());
    }

    /**
     * Waiter bật/tắt tình trạng còn hàng của món ăn
     * URL: PUT /api/menu/{id}/toggle
     */
    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    public ResponseEntity<?> toggleMenuStatus(@PathVariable Long id) {
        try {
            MenuItem updated = foodOrderService.toggleMenuItemAvailability(id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
