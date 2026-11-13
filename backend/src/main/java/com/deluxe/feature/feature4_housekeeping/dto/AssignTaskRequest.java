package com.deluxe.feature.feature4_housekeeping.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignTaskRequest {
    private Long roomId;
    private Long assignedTo;
    private String note;
}

