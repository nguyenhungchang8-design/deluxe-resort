package com.deluxe.feature.feature4_housekeeping.dto;

import com.deluxe.enums.CleaningStatus;
import com.deluxe.enums.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomToCleanDto {
    private Long roomId;
    private String roomNumber;
    private Long roomTypeId;
    private String roomTypeName;
    private RoomStatus roomStatus;
    private Long taskId;
    private CleaningStatus cleaningStatus;
    private Long assignedTo;
    private String assignedToName;
    private LocalDateTime createdAt;
    private String formattedCreatedAt;
    private String note;
}
