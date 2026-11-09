package com.deluxe.repository;

import com.deluxe.entity.FoodOrder;
import com.deluxe.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodOrderRepository extends JpaRepository<FoodOrder, Long> {
    List<FoodOrder> findByCustomerId(Long customerId);
    List<FoodOrder> findByStatus(OrderStatus status);
    List<FoodOrder> findByRoomId(Long roomId);
}
