package com.deluxe.feature.feature3_food.dto;

import com.deluxe.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodOrderDetailDto {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long roomId;
    private String roomNumber;
    private OrderStatus status;
    private String note;
    private LocalDateTime createdAt;
    private BigDecimal totalAmount;
    private List<OrderItemDetailDto> items;
}
