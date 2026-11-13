package com.deluxe.feature.feature4_housekeeping.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportMaintenanceRequest {
    private Long roomId;
    private String description;
    private String severity;
}

