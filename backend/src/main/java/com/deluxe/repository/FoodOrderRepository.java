package com.deluxe.repository;

import com.deluxe.entity.FoodOrder;
import com.deluxe.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý đơn gọi món ăn (Feature 3 - Nguyễn Lâm Phúc)
 */
@Repository
public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {

    /**
     * Lấy các đơn gọi món của một khách hàng, sắp xếp theo thời gian mới nhất
     */
    List<FoodOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    /**
     * Tìm theo khách hàng
     */
    List<FoodOrder> findByCustomerId(Long customerId);

    /**
     * Tìm các đơn theo trạng thái (NEW, PREPARING, SERVING, COMPLETED...)
     */
    List<FoodOrder> findByStatus(OrderStatus status);

    /**
     * Tìm các đơn gọi món theo phòng
     */
    List<FoodOrder> findByRoomId(Long roomId);

    /**
     * Tìm các đơn gọi món theo phòng mà chưa bị hủy (dùng tính tiền phòng cho Feature 5)
     */
    List<FoodOrder> findByRoomIdAndStatusNot(Long roomId, OrderStatus status);
}
