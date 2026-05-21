package com.example.EDIP.document.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;
@Getter
@Setter
public class ForwardToColleagueRequest {

    @NotNull(message = "Colleague email is required")
    private String colleagueEmail;
    private String notes;
}
