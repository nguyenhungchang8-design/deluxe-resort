package com.deluxe.feature.feature2_booking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BookingRequest {

    /** Optional: chỉ dùng khi ADMIN/RECEPTIONIST đặt phòng hộ khách hàng */
    private Long customerId;

    @NotNull(message = "Phòng không được để trống")
    private Long roomId;

    @NotNull(message = "Ngày nhận phòng không được để trống")
    private LocalDate checkIn;

    @NotNull(message = "Ngày trả phòng không được để trống")
    @Future(message = "Ngày trả phòng phải là ngày trong tương lai")
    private LocalDate checkOut;

    @DecimalMin(value = "0.0", message = "Tiền cọc không được âm")
    private BigDecimal deposit;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public BigDecimal getDeposit() { return deposit; }
    public void setDeposit(BigDecimal deposit) { this.deposit = deposit; }
}
