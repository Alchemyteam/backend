package com.ecosystem.dto.punchout;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * cXML响应包装类（用于错误响应）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CxmlResponse {
    private String xmlContent;
    private int statusCode;
    private String statusText;
}

