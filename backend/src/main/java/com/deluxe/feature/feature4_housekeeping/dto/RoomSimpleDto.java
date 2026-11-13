package com.deluxe.feature.feature4_housekeeping.dto;

import com.deluxe.enums.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomSimpleDto {
    private Long id;
    private String roomNumber;
    private String roomTypeName;
    private RoomStatus status;
}

