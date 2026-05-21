package com.example.EDIP.template.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ResignationTemplateDto {

    private LocalDate resignationDate;
    private LocalDate lastWorkingDay;
    private String reason;
    private Integer noticePeriod;

}