package com.deluxe.feature.feature4_housekeeping.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HousekeepingStaffDto {
    private Long id;
    private String username;
    private String fullName;
}

