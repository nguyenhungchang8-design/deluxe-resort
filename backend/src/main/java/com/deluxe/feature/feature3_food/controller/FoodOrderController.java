package com.deluxe.feature.feature3_food.controller;

import com.deluxe.entity.User;
import com.deluxe.feature.feature3_food.dto.CreateOrderRequest;
import com.deluxe.feature.feature3_food.dto.FoodOrderDetailDto;
import com.deluxe.feature.feature3_food.dto.UpdateOrderStatusRequest;
import com.deluxe.feature.feature3_food.service.FoodOrderService;
import com.deluxe.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Controller quản lý việc gọi món ăn và cập nhật trạng thái đơn (UC16 -> UC20)
 * Người thực hiện: Nguyễn Lâm Phúc (Member 3)
 */
@RestController
@RequestMapping("/api/orders")
public class FoodOrderController {

    private final FoodOrderService foodOrderService;
    private final UserRepository userRepository;

    public FoodOrderController(FoodOrderService foodOrderService, UserRepository userRepository) {
        this.foodOrderService = foodOrderService;
        this.userRepository = userRepository;
    }

    /**
     * UC16: Khách hàng tạo đơn đặt món ăn
     * URL: POST /api/orders
     */
    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody CreateOrderRequest request, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập trước khi gọi món."));
        }

        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Không tìm thấy tài khoản người dùng."));
        }

        try {
            FoodOrderDetailDto order = foodOrderService.createOrder(user.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(order);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * UC17: Khách hàng xem danh sách các đơn gọi món của mình
     * URL: GET /api/orders/my-orders
     */
    @GetMapping("/my-orders")
    public ResponseEntity<?> getMyOrders(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập."));
        }

        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Không tìm thấy người dùng."));
        }

        List<FoodOrderDetailDto> list = foodOrderService.getOrdersByCustomer(user.getId());
        return ResponseEntity.ok(list);
    }

    /**
     * UC17: Xem chi tiết một đơn gọi món
     * URL: GET /api/orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderDetail(@PathVariable Long id) {
        try {
            FoodOrderDetailDto detail = foodOrderService.getOrderDetail(id);
            return ResponseEntity.ok(detail);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * UC18: Khách hàng hủy đơn đặt món theo quy định (khi còn là NEW)
     * URL: PUT /api/orders/{id}/cancel
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Vui lòng đăng nhập."));
        }

        User user = userRepository.findByUsername(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Không tìm thấy người dùng."));
        }

        try {
            FoodOrderDetailDto cancelled = foodOrderService.cancelOrder(id, user.getId());
            return ResponseEntity.ok(cancelled);
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Dành cho nhân viên phục vụ (Waiter): Xem toàn bộ đơn gọi món trong khách sạn
     * URL: GET /api/orders/waiter
     */
    @GetMapping("/waiter")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    public ResponseEntity<List<FoodOrderDetailDto>> getAllOrdersForWaiter() {
        return ResponseEntity.ok(foodOrderService.getAllOrders());
    }

    /**
     * UC19: Waiter xác nhận đơn gọi món của khách
     * URL: PUT /api/orders/{id}/confirm
     */
    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    public ResponseEntity<?> confirmOrder(@PathVariable Long id) {
        try {
            FoodOrderDetailDto updated = foodOrderService.confirmOrder(id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * UC20: Waiter cập nhật trạng thái chuẩn bị và phục vụ (PREPARING, SERVING, COMPLETED, CANCELLED)
     * URL: PUT /api/orders/{id}/status
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
        try {
            FoodOrderDetailDto updated = foodOrderService.updateOrderStatus(id, request.getStatus());
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Hỗ trợ Feature 5: Tính tổng tiền các món ăn theo phòng để xuất hóa đơn check-out
     * URL: GET /api/orders/room/{roomId}/total
     */
    @GetMapping("/room/{roomId}/total")
    public ResponseEntity<Map<String, Object>> getFoodChargeByRoom(@PathVariable Long roomId) {
        BigDecimal total = foodOrderService.calculateTotalFoodChargeByRoom(roomId);
        return ResponseEntity.ok(Map.of(
                "roomId", roomId,
                "foodCharge", total
        ));
    }
}
