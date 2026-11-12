package com.deluxe.feature.feature2_booking.dto;

import com.deluxe.enums.RoomStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RoomRequest {

    @NotBlank(message = "Số phòng không được để trống")
    private String roomNumber;

    @NotNull(message = "Hạng phòng không được để trống")
    private Long roomTypeId;

    private RoomStatus status;

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public Long getRoomTypeId() { return roomTypeId; }
    public void setRoomTypeId(Long roomTypeId) { this.roomTypeId = roomTypeId; }
    public RoomStatus getStatus() { return status; }
    public void setStatus(RoomStatus status) { this.status = status; }
}

