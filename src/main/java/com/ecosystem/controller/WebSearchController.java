package com.ecosystem.controller;

import com.ecosystem.dto.chat.MaterialSearchCriteria;
import com.ecosystem.dto.search.WebSearchResponse;
import com.ecosystem.service.LLMSearchParser;
import com.ecosystem.service.MaterialSearchService;
import com.ecosystem.service.WebSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Web 搜索控制器
 * 提供增强的搜索功能，支持内部数据库搜索和 Web 搜索回退
 */
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Slf4j
public class WebSearchController {

    private final MaterialSearchService materialSearchService;
    private final WebSearchService webSearchService;
    private final LLMSearchParser llmSearchParser;

    /**
     * 智能搜索（支持 Web 回退）
     * 当内部数据库无结果时，自动使用 Tavily Search 进行网络搜索
     * 
     * @param query 搜索查询（自然语言）
     * @param webFallback 是否启用 Web 回退（默认 true）
     * @return WebSearchResponse 包含来源类型和搜索结果
     */
    @GetMapping("/smart")
    public ResponseEntity<WebSearchResponse> smartSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "true") boolean webFallback) {
        
        log.info("🔍 Smart search request: query='{}', webFallback={}", query, webFallback);
        
        try {
            // 使用 LLM 解析自然语言查询
            MaterialSearchCriteria criteria = llmSearchParser.parseSearchQuery(query);
            
            WebSearchResponse response;
            if (webFallback) {
                // 启用 Web 回退
                response = materialSearchService.searchWithWebFallback(criteria);
            } else {
                // 仅内部搜索
                response = WebSearchResponse.internalResult(
                        query,
                        materialSearchService.searchWithWebFallback(criteria).getResults(),
                        0L
                );
            }
            
            log.info("✅ Smart search completed: source={}, results={}", 
                    response.getSource(), response.getTotalResults());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Smart search failed: {}", e.getMessage(), e);
            return ResponseEntity.ok(WebSearchResponse.errorResult(query, e.getMessage()));
        }
    }

    /**
     * 混合搜索（内部 + Web 增强）
     * 不管内部是否有结果，都会尝试 Web 搜索来补充
     * 
     * @param query 搜索查询
     * @param maxWebResults 最大 Web 结果数（默认 5）
     * @return WebSearchResponse 混合搜索结果
     */
    @GetMapping("/enhanced")
    public ResponseEntity<WebSearchResponse> enhancedSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int maxWebResults) {
        
        log.info("🔍 Enhanced search request: query='{}', maxWebResults={}", query, maxWebResults);
        
        try {
            MaterialSearchCriteria criteria = llmSearchParser.parseSearchQuery(query);
            WebSearchResponse response = materialSearchService.searchWithWebEnhancement(criteria, maxWebResults);
            
            log.info("✅ Enhanced search completed: source={}, internal={}, web={}", 
                    response.getSource(), response.getInternalCount(), response.getWebCount());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Enhanced search failed: {}", e.getMessage(), e);
            return ResponseEntity.ok(WebSearchResponse.errorResult(query, e.getMessage()));
        }
    }

    /**
     * 纯 Web 搜索
     * 直接使用 Tavily Search 进行网络搜索（不查询内部数据库）
     * 
     * @param query 搜索查询
     * @param count 结果数量（默认 10）
     * @param enhance 是否启用页面增强（默认 true）
     * @return WebSearchResponse
     */
    @GetMapping("/web")
    public ResponseEntity<WebSearchResponse> webOnlySearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(defaultValue = "true") boolean enhance) {
        
        log.info("🌐 Web-only search request: query='{}', count={}, enhance={}", query, count, enhance);
        
        try {
            if (!webSearchService.isWebSearchAvailable()) {
                return ResponseEntity.ok(WebSearchResponse.errorResult(query, "Web search is not available"));
            }
            
            WebSearchResponse response = webSearchService.searchWeb(query, count);
            
            log.info("✅ Web search completed: results={}, enhanced={}", 
                    response.getTotalResults(), response.getEnhancementApplied());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Web search failed: {}", e.getMessage(), e);
            return ResponseEntity.ok(WebSearchResponse.errorResult(query, e.getMessage()));
        }
    }

    /**
     * 检查搜索服务状态
     * @return 服务状态信息
     */
    @GetMapping("/status")
    public ResponseEntity<SearchServiceStatus> getServiceStatus() {
        SearchServiceStatus status = new SearchServiceStatus();
        status.setInternalSearchAvailable(true);
        status.setWebSearchAvailable(webSearchService.isWebSearchAvailable());
        status.setMessage("Search service is operational");
        return ResponseEntity.ok(status);
    }

    /**
     * 搜索服务状态 DTO
     */
    public static class SearchServiceStatus {
        private boolean internalSearchAvailable;
        private boolean webSearchAvailable;
        private String message;

        public boolean isInternalSearchAvailable() { return internalSearchAvailable; }
        public void setInternalSearchAvailable(boolean available) { this.internalSearchAvailable = available; }
        public boolean isWebSearchAvailable() { return webSearchAvailable; }
        public void setWebSearchAvailable(boolean available) { this.webSearchAvailable = available; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
