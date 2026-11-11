package com.deluxe.feature.feature4_housekeeping.dto;

import com.deluxe.enums.CleaningStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatusRequest {
    private CleaningStatus status;
}

