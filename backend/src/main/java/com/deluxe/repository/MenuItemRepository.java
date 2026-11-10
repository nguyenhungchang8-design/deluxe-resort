package com.deluxe.repository;

import com.deluxe.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository quản lý thực đơn món ăn (Feature 3 - Nguyễn Lâm Phúc)
 */
@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    /**
     * Lấy danh sách món ăn đang sẵn sàng phục vụ (available = true) cho khách hàng
     */
    List<MenuItem> findByAvailableTrue();

    /**
     * Tìm kiếm món ăn theo tên (không phân biệt hoa thường)
     */
    List<MenuItem> findByNameContainingIgnoreCase(String name);
}
