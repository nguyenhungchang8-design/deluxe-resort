package com.deluxe.feature.feature2_booking.dto;

import com.deluxe.enums.RoomStatus;

import java.math.BigDecimal;

public class RoomDto {
    private Long id;
    private String roomNumber;
    private Long roomTypeId;
    private String roomTypeName;
    private BigDecimal pricePerNight;
    private Integer capacity;
    private String description;
    private RoomStatus status;

    public RoomDto() {
    }

    public RoomDto(Long id, String roomNumber, Long roomTypeId,
            String roomTypeName, RoomStatus status) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.roomTypeId = roomTypeId;
        this.roomTypeName = roomTypeName;
        this.status = status;
    }

    public RoomDto(Long id, String roomNumber, Long roomTypeId,
            String roomTypeName, BigDecimal pricePerNight,
            Integer capacity, String description, RoomStatus status) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.roomTypeId = roomTypeId;
        this.roomTypeName = roomTypeName;
        this.pricePerNight = pricePerNight;
        this.capacity = capacity;
        this.description = description;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public Long getRoomTypeId() {
        return roomTypeId;
    }

    public void setRoomTypeId(Long roomTypeId) {
        this.roomTypeId = roomTypeId;
    }

    public String getRoomTypeName() {
        return roomTypeName;
    }

    public void setRoomTypeName(String roomTypeName) {
        this.roomTypeName = roomTypeName;
    }

    public BigDecimal getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(BigDecimal pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }
}
