package com.ecosystem.dto.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Brave Search API 响应 DTO
 * 用于解析 Brave Search API 返回的 JSON 结果
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BraveSearchResult {
    
    /**
     * Web 搜索结果
     */
    private WebResults web;
    
    /**
     * 查询信息
     */
    private Query query;
    
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebResults {
        private List<WebResult> results;
        
        @JsonProperty("moreResultsAvailable")
        private Boolean moreResultsAvailable;
    }
    
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebResult {
        private String title;
        private String url;
        private String description;
        
        @JsonProperty("page_age")
        private String pageAge;
        
        @JsonProperty("page_fetched")
        private String pageFetched;
        
        private Profile profile;
        
        @JsonProperty("meta_url")
        private MetaUrl metaUrl;
        
        private Thumbnail thumbnail;
        
        @JsonProperty("extra_snippets")
        private List<String> extraSnippets;
    }
    
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Profile {
        private String name;
        private String url;
        
        @JsonProperty("long_name")
        private String longName;
        
        private String img;
    }
    
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MetaUrl {
        private String scheme;
        private String netloc;
        private String hostname;
        private String favicon;
        private String path;
    }
    
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Thumbnail {
        private String src;
        private Integer width;
        private Integer height;
    }
    
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Query {
        private String original;
        
        @JsonProperty("show_strict_warning")
        private Boolean showStrictWarning;
        
        @JsonProperty("is_navigational")
        private Boolean isNavigational;
        
        @JsonProperty("local_decision")
        private String localDecision;
        
        @JsonProperty("local_locations_idx")
        private Integer localLocationsIdx;
        
        @JsonProperty("is_news_breaking")
        private Boolean isNewsBreaking;
        
        @JsonProperty("spellcheck_off")
        private Boolean spellcheckOff;
        
        private String country;
        
        @JsonProperty("bad_results")
        private Boolean badResults;
        
        @JsonProperty("should_fallback")
        private Boolean shouldFallback;
        
        private String city;
        
        @JsonProperty("header_country")
        private String headerCountry;
        
        @JsonProperty("more_results_available")
        private Boolean moreResultsAvailable;
        
        private String state;
    }
}
