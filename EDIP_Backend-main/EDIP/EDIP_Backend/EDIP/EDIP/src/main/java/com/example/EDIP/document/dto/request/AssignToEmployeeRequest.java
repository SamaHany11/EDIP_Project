package com.example.EDIP.document.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class AssignToEmployeeRequest {

    @NotNull(message = "Employee User is required")
    private String employeeEmail;
}