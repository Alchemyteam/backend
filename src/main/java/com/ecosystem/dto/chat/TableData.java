package com.ecosystem.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableData {
    private String title; // 表格标题
    private List<String> headers; // 表头
    private List<Map<String, Object>> rows; // 表格行数据
    private String description; // 表格描述
}


