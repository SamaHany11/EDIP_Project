package com.example.EDIP.template.dto;

import lombok.Data;

@Data
public class TransferTemplateDto {

    private String currentDepartment;
    private String targetDepartment;
    private String reason;

}