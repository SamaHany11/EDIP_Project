package com.example.EDIP.document.dto.response;

import java.util.List;
import lombok.Data;

@Data
public class PagedResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
