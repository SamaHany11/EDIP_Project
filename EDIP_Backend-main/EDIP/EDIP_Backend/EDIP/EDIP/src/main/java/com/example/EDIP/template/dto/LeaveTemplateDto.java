package com.example.EDIP.template.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LeaveTemplateDto {

    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private String leaveType;

}