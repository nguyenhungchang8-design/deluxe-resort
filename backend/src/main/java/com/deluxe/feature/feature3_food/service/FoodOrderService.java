package com.deluxe.feature.feature3_food.service;

import com.deluxe.entity.FoodOrder;
import com.deluxe.entity.MenuItem;
import com.deluxe.entity.OrderItem;
import com.deluxe.entity.Room;
import com.deluxe.entity.User;
import com.deluxe.enums.OrderStatus;
import com.deluxe.feature.feature3_food.dto.CreateOrderRequest;
import com.deluxe.feature.feature3_food.dto.FoodOrderDetailDto;
import com.deluxe.feature.feature3_food.dto.OrderItemDetailDto;
import com.deluxe.feature.feature3_food.dto.OrderItemRequest;
import com.deluxe.repository.FoodOrderRepository;
import com.deluxe.repository.MenuItemRepository;
import com.deluxe.repository.OrderItemRepository;
import com.deluxe.repository.RoomRepository;
import com.deluxe.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service xử lý toàn bộ nghiệp vụ cho Feature 3: Food Menu & Ordering
 * Người thực hiện: Nguyễn Lâm Phúc (Member 3)
 */
@Service
public class FoodOrderService {

    private final MenuItemRepository menuItemRepository;
    private final FoodOrderRepository foodOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;

    public FoodOrderService(MenuItemRepository menuItemRepository,
                            FoodOrderRepository foodOrderRepository,
                            OrderItemRepository orderItemRepository,
                            UserRepository userRepository,
                            RoomRepository roomRepository) {
        this.menuItemRepository = menuItemRepository;
        this.foodOrderRepository = foodOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
    }

    // ==========================================
    // 1. QUẢN LÝ THỰC ĐƠN (MENU - UC15)
    // ==========================================

    /**
     * Lấy danh sách món ăn đang phục vụ (available = true) cho khách hàng xem
     */
    public List<MenuItem> getAvailableMenu() {
        return menuItemRepository.findByAvailableTrue();
    }

    /**
     * Lấy toàn bộ thực đơn cho nhân viên phục vụ (Waiter) theo dõi
     */
    public List<MenuItem> getAllMenu() {
        return menuItemRepository.findAll();
    }

    /**
     * Waiter cập nhật trạng thái món ăn (Còn món / Hết món)
     */
    @Transactional
    public MenuItem toggleMenuItemAvailability(Long menuItemId) {
        MenuItem item = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy món ăn với ID: " + menuItemId));
        item.setAvailable(!Boolean.TRUE.equals(item.getAvailable()));
        return menuItemRepository.save(item);
    }

    // ==========================================
    // 2. TẠO & XỬ LÝ ĐƠN ĐẶT MÓN (UC16, UC17, UC18)
    // ==========================================

    /**
     * Khách hàng tạo đơn đặt món ăn (UC16)
     */
    @Transactional
    public FoodOrderDetailDto createOrder(Long customerId, CreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Đơn hàng phải có ít nhất một món ăn.");
        }

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin khách hàng."));

        // Xác định số phòng (nếu khách cung cấp roomId hoặc roomNumber)
        Long targetRoomId = request.getRoomId();
        if (targetRoomId == null && request.getRoomNumber() != null && !request.getRoomNumber().isBlank()) {
            Room room = roomRepository.findByRoomNumber(request.getRoomNumber().trim())
                    .orElse(null);
            if (room != null) {
                targetRoomId = room.getId();
            }
        }

        // Bước 1: Lưu thông tin FoodOrder ban đầu
        FoodOrder order = FoodOrder.builder()
                .customerId(customer.getId())
                .roomId(targetRoomId)
                .status(OrderStatus.NEW)
                .note(request.getNote())
                .createdAt(LocalDateTime.now())
                .build();
        order = foodOrderRepository.save(order);

        // Bước 2: Lưu danh sách chi tiết các món ăn (OrderItem)
        List<OrderItem> savedItems = new ArrayList<>();
        for (OrderItemRequest itemReq : request.getItems()) {
            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                continue;
            }
            MenuItem menuItem = menuItemRepository.findById(itemReq.getMenuItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy món ăn ID: " + itemReq.getMenuItemId()));

            if (!Boolean.TRUE.equals(menuItem.getAvailable())) {
                throw new IllegalStateException("Món '" + menuItem.getName() + "' hiện tại đã hết, vui lòng chọn món khác.");
            }

            OrderItem orderItem = OrderItem.builder()
                    .orderId(order.getId())
                    .menuItemId(menuItem.getId())
                    .quantity(itemReq.getQuantity())
                    .note(itemReq.getNote())
                    .build();

            savedItems.add(orderItemRepository.save(orderItem));
        }

        return mapToDetailDto(order, savedItems);
    }

    /**
     * Khách hàng xem danh sách đơn hàng của chính mình (UC17)
     */
    public List<FoodOrderDetailDto> getOrdersByCustomer(Long customerId) {
        List<FoodOrder> orders = foodOrderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        return orders.stream()
                .map(this::loadOrderDetail)
                .collect(Collectors.toList());
    }

    /**
     * Xem chi tiết 1 đơn gọi món theo ID (UC17)
     */
    public FoodOrderDetailDto getOrderDetail(Long orderId) {
        FoodOrder order = foodOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn món ID: " + orderId));
        return loadOrderDetail(order);
    }

    /**
     * Khách hàng hủy đơn đặt món theo quy định (UC18)
     * Quy tắc: Chỉ được hủy khi đơn ở trạng thái NEW (bếp chưa tiếp nhận/nấu)
     */
    @Transactional
    public FoodOrderDetailDto cancelOrder(Long orderId, Long customerId) {
        FoodOrder order = foodOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn món ID: " + orderId));

        if (!order.getCustomerId().equals(customerId)) {
            throw new SecurityException("Bạn không có quyền hủy đơn đặt món của người khác.");
        }

        if (order.getStatus() != OrderStatus.NEW) {
            throw new IllegalStateException("Không thể hủy đơn hàng vì nhân viên đã tiếp nhận hoặc đang chuẩn bị món.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        FoodOrder updated = foodOrderRepository.save(order);
        return loadOrderDetail(updated);
    }

    // ==========================================
    // 3. NGHIỆP VỤ NHÂN VIÊN PHỤC VỤ (WAITER - UC19, UC20)
    // ==========================================

    /**
     * Waiter xem toàn bộ các đơn gọi món của tất cả các bàn/phòng
     */
    public List<FoodOrderDetailDto> getAllOrders() {
        List<FoodOrder> orders = foodOrderRepository.findAll();
        orders.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return orders.stream()
                .map(this::loadOrderDetail)
                .collect(Collectors.toList());
    }

    /**
     * Waiter xác nhận đơn gọi món (UC19)
     */
    @Transactional
    public FoodOrderDetailDto confirmOrder(Long orderId) {
        return updateOrderStatus(orderId, OrderStatus.CONFIRMED);
    }

    /**
     * Waiter cập nhật trạng thái chuẩn bị và phục vụ (UC20):
     * NEW -> CONFIRMED -> PREPARING -> SERVING -> COMPLETED
     */
    @Transactional
    public FoodOrderDetailDto updateOrderStatus(Long orderId, OrderStatus newStatus) {
        FoodOrder order = foodOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn món ID: " + orderId));

        order.setStatus(newStatus);
        FoodOrder saved = foodOrderRepository.save(order);
        return loadOrderDetail(saved);
    }

    // ==========================================
    // 4. HỖ TRỢ LIÊN FEATURE (FEATURE 5 - TỔNG HỢP TIỀN HÓA ĐƠN)
    // ==========================================

    /**
     * Tính tổng tiền các món ăn của một phòng (dành cho Feature 5 khi kết toán check-out)
     */
    public BigDecimal calculateTotalFoodChargeByRoom(Long roomId) {
        List<FoodOrder> orders = foodOrderRepository.findByRoomIdAndStatusNot(roomId, OrderStatus.CANCELLED);
        BigDecimal total = BigDecimal.ZERO;

        for (FoodOrder order : orders) {
            FoodOrderDetailDto detail = loadOrderDetail(order);
            if (detail.getTotalAmount() != null) {
                total = total.add(detail.getTotalAmount());
            }
        }
        return total;
    }

    // ==========================================
    // HÀM TIỆN ÍCH CHUYỂN ĐỔI ENTITY SANG DTO
    // ==========================================

    private FoodOrderDetailDto loadOrderDetail(FoodOrder order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        return mapToDetailDto(order, items);
    }

    private FoodOrderDetailDto mapToDetailDto(FoodOrder order, List<OrderItem> items) {
        // Lấy thông tin khách hàng
        String customerName = "Khách hàng #" + order.getCustomerId();
        User customer = userRepository.findById(order.getCustomerId()).orElse(null);
        if (customer != null && customer.getFullName() != null && !customer.getFullName().isBlank()) {
            customerName = customer.getFullName();
        }

        // Lấy thông tin số phòng
        String roomNumber = "Tại bàn / Mang đi";
        if (order.getRoomId() != null) {
            Room room = roomRepository.findById(order.getRoomId()).orElse(null);
            if (room != null) {
                roomNumber = room.getRoomNumber();
            }
        }

        // Lấy thông tin chi tiết từng món và tính tổng tiền
        BigDecimal grandTotal = BigDecimal.ZERO;
        List<OrderItemDetailDto> itemDtos = new ArrayList<>();

        if (items != null && !items.isEmpty()) {
            List<Long> menuItemIds = items.stream().map(OrderItem::getMenuItemId).toList();
            Map<Long, MenuItem> menuItemMap = menuItemRepository.findAllById(menuItemIds).stream()
                    .collect(Collectors.toMap(MenuItem::getId, m -> m));

            for (OrderItem oi : items) {
                MenuItem mi = menuItemMap.get(oi.getMenuItemId());
                String itemName = (mi != null) ? mi.getName() : "Món không xác định";
                BigDecimal price = (mi != null && mi.getPrice() != null) ? mi.getPrice() : BigDecimal.ZERO;
                BigDecimal subTotal = price.multiply(BigDecimal.valueOf(oi.getQuantity()));
                grandTotal = grandTotal.add(subTotal);

                itemDtos.add(OrderItemDetailDto.builder()
                        .id(oi.getId())
                        .menuItemId(oi.getMenuItemId())
                        .menuItemName(itemName)
                        .price(price)
                        .quantity(oi.getQuantity())
                        .subTotal(subTotal)
                        .note(oi.getNote())
                        .build());
            }
        }

        return FoodOrderDetailDto.builder()
                .id(order.getId())
                .customerId(order.getCustomerId())
                .customerName(customerName)
                .roomId(order.getRoomId())
                .roomNumber(roomNumber)
                .status(order.getStatus())
                .note(order.getNote())
                .createdAt(order.getCreatedAt())
                .totalAmount(grandTotal)
                .items(itemDtos)
                .build();
    }
}
