package com.ecosystem.dto.search;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统一搜索响应 DTO
 * 包含内部搜索和 Web 搜索的合并结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSearchResponse {
    
    /**
     * 搜索来源类型
     */
    public enum SourceType {
        INTERNAL,       // 仅内部数据库结果
        WEB_FALLBACK,   // 内部无结果，使用 Web 搜索
        MIXED           // 混合结果（内部 + Web 补充）
    }
    
    // 原始查询
    private String query;
    
    // 来源类型
    private SourceType source;
    
    // 搜索结果列表
    private List<ProductCard> results;
    
    // 统计信息
    private Integer totalResults;
    private Integer internalCount;      // 内部结果数量
    private Integer webCount;           // Web 结果数量
    
    // 搜索元数据
    private Long searchTimeMs;          // 搜索耗时（毫秒）
    private String searchEngine;        // 使用的搜索引擎（内部/Tavily）
    private Boolean enhancementApplied; // 是否应用了增强处理
    
    // 错误信息（如果有）
    private String errorMessage;
    
    /**
     * 创建内部搜索结果响应
     */
    public static WebSearchResponse internalResult(String query, List<ProductCard> results, long searchTimeMs) {
        return WebSearchResponse.builder()
                .query(query)
                .source(SourceType.INTERNAL)
                .results(results)
                .totalResults(results.size())
                .internalCount(results.size())
                .webCount(0)
                .searchTimeMs(searchTimeMs)
                .searchEngine("internal")
                .enhancementApplied(false)
                .build();
    }
    
    /**
     * 创建 Web 回退搜索结果响应
     */
    public static WebSearchResponse webFallbackResult(String query, List<ProductCard> results, 
                                                       long searchTimeMs, boolean enhanced) {
        return WebSearchResponse.builder()
                .query(query)
                .source(SourceType.WEB_FALLBACK)
                .results(results)
                .totalResults(results.size())
                .internalCount(0)
                .webCount(results.size())
                .searchTimeMs(searchTimeMs)
                .searchEngine("tavily")
                .enhancementApplied(enhanced)
                .build();
    }
    
    /**
     * 创建混合搜索结果响应
     */
    public static WebSearchResponse mixedResult(String query, List<ProductCard> results,
                                                 int internalCount, int webCount,
                                                 long searchTimeMs, boolean enhanced) {
        return WebSearchResponse.builder()
                .query(query)
                .source(SourceType.MIXED)
                .results(results)
                .totalResults(results.size())
                .internalCount(internalCount)
                .webCount(webCount)
                .searchTimeMs(searchTimeMs)
                .searchEngine("internal+tavily")
                .enhancementApplied(enhanced)
                .build();
    }
    
    /**
     * 创建错误响应
     */
    public static WebSearchResponse errorResult(String query, String errorMessage) {
        return WebSearchResponse.builder()
                .query(query)
                .source(SourceType.INTERNAL)
                .results(List.of())
                .totalResults(0)
                .errorMessage(errorMessage)
                .build();
    }
}
