package com.deluxe.feature.feature4_housekeeping.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceResponseDto {
    private Long id;
    private Long roomId;
    private String roomNumber;
    private String roomTypeName;
    private Long reportedBy;
    private String reportedByName;
    private String description;
    private String severity;
    private String status;
    private LocalDateTime createdAt;
    private String formattedCreatedAt;
}

