package com.deluxe.feature.feature2_booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class RoomTypeRequest {

    @NotBlank(message = "Tên hạng phòng không được để trống")
    private String name;

    @NotNull(message = "Giá mỗi đêm không được để trống")
    @Positive(message = "Giá phải là số dương")
    private BigDecimal pricePerNight;

    @Positive(message = "Sức chứa phải là số dương")
    private Integer capacity;

    private String description;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(BigDecimal pricePerNight) { this.pricePerNight = pricePerNight; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

