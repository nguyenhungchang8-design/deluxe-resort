package com.deluxe.repository;

import com.deluxe.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý chi tiết từng món ăn trong đơn gọi món (Feature 3 - Nguyễn Lâm Phúc)
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Lấy danh sách các món ăn thuộc về một đơn gọi món (orderId)
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Xóa các món thuộc đơn hàng khi cần thiết
     */
    void deleteByOrderId(Long orderId);
}
