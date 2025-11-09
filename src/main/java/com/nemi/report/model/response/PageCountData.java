package com.nemi.report.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageCountData {
    private Long totalElements;
    private Integer totalPages;
    private Integer numberOfElements;
}
