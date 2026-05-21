package com.example.EDIP.search.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultWrapper<T> {

    private List<T> data;

    private long count; // total elements in DB

    private int page;

    private int size;

    private int totalPages;
}