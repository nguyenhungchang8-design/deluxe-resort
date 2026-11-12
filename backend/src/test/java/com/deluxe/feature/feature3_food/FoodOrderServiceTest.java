package com.deluxe.feature.feature3_food;

import com.deluxe.entity.FoodOrder;
import com.deluxe.entity.MenuItem;
import com.deluxe.entity.OrderItem;
import com.deluxe.entity.User;
import com.deluxe.enums.OrderStatus;
import com.deluxe.feature.feature3_food.dto.CreateOrderRequest;
import com.deluxe.feature.feature3_food.dto.FoodOrderDetailDto;
import com.deluxe.feature.feature3_food.dto.OrderItemRequest;
import com.deluxe.feature.feature3_food.service.FoodOrderService;
import com.deluxe.repository.FoodOrderRepository;
import com.deluxe.repository.MenuItemRepository;
import com.deluxe.repository.OrderItemRepository;
import com.deluxe.repository.RoomRepository;
import com.deluxe.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test toàn diện cho Feature 3: Food Menu & Ordering
 * Người thực hiện: Nguyễn Lâm Phúc (Member 3 - HE194784)
 * Kiểm thử chi tiết các Use Cases:
 * - UC15: Quản lý thực đơn (Xem menu, toggle món có sẵn)
 * - UC16: Khách đặt món (Tạo đơn, tính tiền, chặn món hết)
 * - UC17: Xem danh sách và chi tiết đơn món
 * - UC18: Hủy đơn theo quy định (Chỉ hủy khi NEW)
 * - UC19: Waiter xác nhận đơn
 * - UC20: Cập nhật tiến độ món (PREPARING, SERVING, COMPLETED)
 */
@ExtendWith(MockitoExtension.class)
class FoodOrderServiceTest {

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private FoodOrderRepository foodOrderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoomRepository roomRepository;

    @InjectMocks
    private FoodOrderService foodOrderService;

    private User sampleCustomer;
    private MenuItem sampleItemAvailable;
    private MenuItem sampleItemUnavailable;

    @BeforeEach
    void setUp() {
        sampleCustomer = User.builder()
                .id(10L)
                .username("customer01")
                .fullName("Nguyen Van Khach")
                .build();

        sampleItemAvailable = MenuItem.builder()
                .id(1L)
                .name("Phở Bò Đặc Biệt")
                .price(BigDecimal.valueOf(85000))
                .description("Món chính đặc biệt")
                .available(true)
                .build();

        sampleItemUnavailable = MenuItem.builder()
                .id(2L)
                .name("Cua Hoàng Đế Hấp")
                .price(BigDecimal.valueOf(1500000))
                .description("Hải sản tươi sống")
                .available(false)
                .build();
    }

    // ==========================================
    // Test Case: UC15 - Menu Management
    // ==========================================

    @Test
    @DisplayName("UC15: Lấy danh sách thực đơn đang phục vụ")
    void testGetAvailableMenu() {
        when(menuItemRepository.findByAvailableTrue()).thenReturn(List.of(sampleItemAvailable));

        List<MenuItem> result = foodOrderService.getAvailableMenu();

        assertEquals(1, result.size());
        assertEquals("Phở Bò Đặc Biệt", result.get(0).getName());
        assertTrue(result.get(0).getAvailable());
    }

    @Test
    @DisplayName("UC15: Nhân viên Waiter bật / tắt trạng thái món ăn")
    void testToggleMenuItemAvailability() {
        when(menuItemRepository.findById(1L)).thenReturn(Optional.of(sampleItemAvailable));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(i -> i.getArgument(0));

        MenuItem toggled = foodOrderService.toggleMenuItemAvailability(1L);

        assertFalse(toggled.getAvailable(), "Món ăn phải chuyển từ available=true sang false");
    }

    // ==========================================
    // Test Case: UC16 - Create Food Order
    // ==========================================

    @Test
    @DisplayName("UC16: Đặt món thành công khi món ăn còn hàng")
    void testCreateOrder_Success() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .note("Ít cay")
                .items(List.of(
                        OrderItemRequest.builder().menuItemId(1L).quantity(2).build()
                ))
                .build();

        FoodOrder savedOrder = FoodOrder.builder()
                .id(100L)
                .customerId(10L)
                .status(OrderStatus.NEW)
                .createdAt(LocalDateTime.now())
                .build();

        OrderItem savedOrderItem = OrderItem.builder()
                .id(50L)
                .orderId(100L)
                .menuItemId(1L)
                .quantity(2)
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleCustomer));
        when(foodOrderRepository.save(any(FoodOrder.class))).thenReturn(savedOrder);
        when(menuItemRepository.findById(1L)).thenReturn(Optional.of(sampleItemAvailable));
        when(menuItemRepository.findAllById(any())).thenReturn(List.of(sampleItemAvailable));
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(savedOrderItem);

        FoodOrderDetailDto result = foodOrderService.createOrder(10L, request);

        assertNotNull(result);
        assertEquals(OrderStatus.NEW, result.getStatus());
        assertEquals(1, result.getItems().size());
        assertEquals(BigDecimal.valueOf(170000), result.getTotalAmount(), "85.000 x 2 = 170.000 VND");
    }

    @Test
    @DisplayName("UC16: Đặt món thất bại nếu chọn món đã hết (available = false)")
    void testCreateOrder_ItemUnavailable() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .items(List.of(
                        OrderItemRequest.builder().menuItemId(2L).quantity(1).build()
                ))
                .build();

        FoodOrder tempOrder = FoodOrder.builder().id(101L).customerId(10L).build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleCustomer));
        when(foodOrderRepository.save(any(FoodOrder.class))).thenReturn(tempOrder);
        when(menuItemRepository.findById(2L)).thenReturn(Optional.of(sampleItemUnavailable));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                foodOrderService.createOrder(10L, request));

        assertTrue(ex.getMessage().contains("đã hết"));
    }

    // ==========================================
    // Test Case: UC18 - Cancel Order Rule
    // ==========================================

    @Test
    @DisplayName("UC18: Khách hủy đơn thành công khi đơn còn mới (status = NEW)")
    void testCancelOrder_SuccessWhenNew() {
        FoodOrder order = FoodOrder.builder()
                .id(200L)
                .customerId(10L)
                .status(OrderStatus.NEW)
                .createdAt(LocalDateTime.now())
                .build();

        when(foodOrderRepository.findById(200L)).thenReturn(Optional.of(order));
        when(foodOrderRepository.save(any(FoodOrder.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleCustomer));
        when(orderItemRepository.findByOrderId(200L)).thenReturn(List.of());

        FoodOrderDetailDto cancelled = foodOrderService.cancelOrder(200L, 10L);

        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    @DisplayName("UC18: Chặn hủy đơn khi bếp đã tiếp nhận hoặc đang nấu (status != NEW)")
    void testCancelOrder_BlockedWhenPreparing() {
        FoodOrder order = FoodOrder.builder()
                .id(201L)
                .customerId(10L)
                .status(OrderStatus.PREPARING)
                .build();

        when(foodOrderRepository.findById(201L)).thenReturn(Optional.of(order));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                foodOrderService.cancelOrder(201L, 10L));

        assertTrue(ex.getMessage().contains("Không thể hủy đơn hàng"));
    }

    // ==========================================
    // Test Case: UC19 & UC20 - Waiter Workflow
    // ==========================================

    @Test
    @DisplayName("UC19: Waiter xác nhận đơn gọi món (chuyển sang CONFIRMED)")
    void testConfirmOrder() {
        FoodOrder order = FoodOrder.builder()
                .id(300L)
                .customerId(10L)
                .status(OrderStatus.NEW)
                .createdAt(LocalDateTime.now())
                .build();

        when(foodOrderRepository.findById(300L)).thenReturn(Optional.of(order));
        when(foodOrderRepository.save(any(FoodOrder.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleCustomer));
        when(orderItemRepository.findByOrderId(300L)).thenReturn(List.of());

        FoodOrderDetailDto confirmed = foodOrderService.confirmOrder(300L);

        assertEquals(OrderStatus.CONFIRMED, confirmed.getStatus());
    }

    @Test
    @DisplayName("UC20: Waiter cập nhật quy trình phục vụ món ăn (CONFIRMED -> PREPARING -> COMPLETED)")
    void testUpdateOrderStatus() {
        FoodOrder order = FoodOrder.builder()
                .id(301L)
                .customerId(10L)
                .status(OrderStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();

        when(foodOrderRepository.findById(301L)).thenReturn(Optional.of(order));
        when(foodOrderRepository.save(any(FoodOrder.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleCustomer));
        when(orderItemRepository.findByOrderId(301L)).thenReturn(List.of());

        FoodOrderDetailDto preparing = foodOrderService.updateOrderStatus(301L, OrderStatus.PREPARING);
        assertEquals(OrderStatus.PREPARING, preparing.getStatus());

        FoodOrderDetailDto completed = foodOrderService.updateOrderStatus(301L, OrderStatus.COMPLETED);
        assertEquals(OrderStatus.COMPLETED, completed.getStatus());
    }
}
