package com.deluxe.feature.feature2_booking.dto;

import java.math.BigDecimal;

public class RoomTypeDto {
    private Long id;
    private String name;
    private BigDecimal pricePerNight;
    private Integer capacity;
    private String description;

    public RoomTypeDto() {}

    public RoomTypeDto(Long id, String name, BigDecimal pricePerNight,
                       Integer capacity, String description) {
        this.id = id;
        this.name = name;
        this.pricePerNight = pricePerNight;
        this.capacity = capacity;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(BigDecimal pricePerNight) { this.pricePerNight = pricePerNight; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

