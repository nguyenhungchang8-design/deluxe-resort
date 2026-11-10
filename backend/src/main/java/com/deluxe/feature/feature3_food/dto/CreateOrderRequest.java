package com.deluxe.feature.feature3_food.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {
    private Long roomId;
    private String roomNumber;
    private String note;

    @NotEmpty(message = "Danh sách món ăn không được rỗng")
    @Valid
    private List<OrderItemRequest> items;
}
