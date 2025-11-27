package com.ecosystem.dto.buyer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginationResponse {
    private Integer page;
    private Integer limit;
    private Long total;
    private Integer totalPages;
}

