package com.ecosystem.service;

import com.ecosystem.dto.chat.*;
import com.ecosystem.entity.Product;
import com.ecosystem.entity.SalesData;
import com.ecosystem.entity.ProductMaster;
import com.ecosystem.repository.ProductRepository;
import com.ecosystem.repository.SalesDataRepository;
import com.ecosystem.repository.ProductMasterRepository;
import com.ecosystem.service.MaterialSearchService;
import com.ecosystem.service.LLMSearchParser;
import com.ecosystem.dto.chat.MaterialSearchCriteria;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ProductRepository productRepository;
    private final SalesDataRepository salesDataRepository;
    private final MaterialSearchService materialSearchService;
    private final LLMSearchParser llmSearchParser;
    private final RestTemplate restTemplate;
    private final CohereEmbeddingService cohereEmbeddingService;
    private final QdrantService qdrantService;
    private final ProductMasterRepository productMasterRepository;

    @Value("${llm.api.url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String llmApiUrl;

    @Value("${llm.api.key:}")
    private String llmApiKey;

    @Value("${llm.model:gemini-pro}")
    private String llmModel;

    public ChatResponse processMessage(String message, String conversationId) {
        try {
            log.info("Processing chat message: {}", message);
            
            // 1. 分析用户意图
            String intent = analyzeIntent(message);
            log.info("Detected intent: {}", intent);
            
            // 2. 根据意图执行相应操作
            ChatResponse response;
            
            switch (intent) {
                case "CREATE_REQUISITION":
                    response = handleCreateRequisition(message);
                    break;
                case "SEARCH_PRODUCTS":
                    response = handleSearchProducts(message);
                    break;
                case "GET_PRODUCT_INFO":
                    response = handleGetProductInfo(message);
                    break;
                case "COMPARE_PRODUCTS":
                    response = handleCompareProducts(message);
                    break;
                default:
                    response = handleGeneralQuery(message);
            }
            
            // 确保 conversationId 被设置
            if (response.getConversationId() == null) {
                response.setConversationId(conversationId != null ? conversationId : UUID.randomUUID().toString());
            }
            
            log.info("Response - hasTableData: {}, hasActionData: {}", 
                response.getTableData() != null, response.getActionData() != null);
            
            return response;
            
        } catch (Exception e) {
            log.error("Error processing chat message", e);
            return createErrorResponse("Sorry, I encountered an error processing your request.");
        }
    }

    private String analyzeIntent(String message) {
        String lowerMessage = message.toLowerCase();
        log.debug("Analyzing intent for message: {}", lowerMessage);
        
        // 检查是否包含产品相关关键词（扩展关键词列表）
        boolean hasProductKeyword = lowerMessage.matches(".*\\b(battery|steel|formwork|concrete|product|item|material|safety|shoe|shoes|equipment|filter|chemical|tool|device|meter|clamp|leakage|fluke|aet|air|liquide)\\b.*");
        
        if ((lowerMessage.contains("create") || lowerMessage.contains("make")) && 
            (lowerMessage.contains("requisition") || lowerMessage.contains("purchase") || lowerMessage.contains("order"))) {
            log.debug("Intent: CREATE_REQUISITION");
            return "CREATE_REQUISITION";
        } else if (lowerMessage.contains("search") || lowerMessage.contains("find") || 
                   lowerMessage.contains("show") || lowerMessage.contains("list") ||
                   lowerMessage.contains("查找") || lowerMessage.contains("搜索") ||
                   lowerMessage.contains("显示") || lowerMessage.contains("列出")) {
            log.debug("Intent: SEARCH_PRODUCTS");
            return "SEARCH_PRODUCTS";
        } else if (lowerMessage.contains("info") || lowerMessage.contains("detail") || 
                   lowerMessage.contains("about") || lowerMessage.contains("tell me")) {
            log.debug("Intent: GET_PRODUCT_INFO");
            return "GET_PRODUCT_INFO";
        } else if (lowerMessage.contains("compare") || lowerMessage.contains("vs") || 
                   lowerMessage.contains("versus")) {
            log.debug("Intent: COMPARE_PRODUCTS");
            return "COMPARE_PRODUCTS";
        } else if (hasProductKeyword) {
            // 如果包含产品关键词但没有明确的动作，默认搜索产品
            log.debug("Intent: SEARCH_PRODUCTS (inferred from product keyword)");
            return "SEARCH_PRODUCTS";
        }
        
        // 如果查询看起来像是一个产品名称或关键词，也尝试搜索
        // 例如 "Safety Shoes" 应该被识别为搜索
        if (message.trim().split("\\s+").length <= 5 && 
            !lowerMessage.contains("?") && 
            !lowerMessage.contains("how") && 
            !lowerMessage.contains("what") && 
            !lowerMessage.contains("why")) {
            log.debug("Intent: SEARCH_PRODUCTS (inferred from short query)");
            return "SEARCH_PRODUCTS";
        }
        
        log.debug("Intent: GENERAL");
        return "GENERAL";
    }

    private ChatResponse handleCreateRequisition(String message) {
        // 提取产品关键词
        String productKeyword = extractProductKeyword(message);
        log.info("Extracted product keyword: {}", productKeyword);
        
        // 搜索匹配的产品
        List<Product> products = searchProductsByKeyword(productKeyword);
        log.info("Found {} products for keyword: {}", products.size(), productKeyword);
        
        if (products.isEmpty()) {
            // 即使没有找到产品，也返回一个包含空表格的响应
            ChatResponse response = new ChatResponse();
            response.setResponse("I couldn't find any products matching \"" + productKeyword + "\". Please try a different search term.");
            
            TableData tableData = new TableData();
            tableData.setTitle("Purchase Requisition - " + productKeyword);
            tableData.setHeaders(Arrays.asList("Product Name", "Price", "Currency", "Stock", "Seller", "Category"));
            tableData.setRows(Collections.emptyList());
            tableData.setDescription("No products found matching your search.");
            response.setTableData(tableData);
            
            ActionData actionData = new ActionData();
            actionData.setActionType("CREATE_REQUISITION");
            actionData.setParameters(Map.of("productKeyword", productKeyword, "productCount", 0));
            actionData.setMessage("No products found for requisition.");
            response.setActionData(actionData);
            
            return response;
        }
        
        // 构建表格数据
        TableData tableData = new TableData();
        tableData.setTitle("Purchase Requisition - " + productKeyword);
        tableData.setHeaders(Arrays.asList("Product Name", "Price", "Currency", "Stock", "Seller", "Category"));
        
        List<Map<String, Object>> rows = products.stream()
            .map(product -> {
                Map<String, Object> row = new HashMap<>();
                row.put("Product Name", product.getName());
                row.put("Price", product.getPrice());
                row.put("Currency", product.getCurrency());
                row.put("Stock", product.getStock());
                row.put("Seller", product.getSeller() != null ? product.getSeller().getName() : "N/A");
                row.put("Category", product.getCategory() != null ? product.getCategory() : "N/A");
                return row;
            })
            .collect(Collectors.toList());
        
        tableData.setRows(rows);
        tableData.setDescription("Found " + products.size() + " product(s) matching your search.");
        
        // 构建操作数据
        ActionData actionData = new ActionData();
        actionData.setActionType("CREATE_REQUISITION");
        Map<String, Object> params = new HashMap<>();
        params.put("productKeyword", productKeyword);
        params.put("productCount", products.size());
        params.put("productIds", products.stream().map(Product::getId).collect(Collectors.toList()));
        actionData.setParameters(params);
        actionData.setMessage("Purchase requisition created for " + products.size() + " product(s).");
        
        // 调用LLM生成回复文本（如果失败，使用默认回复）
        String llmResponse;
        try {
            llmResponse = callLLM(generateRequisitionPrompt(message, products));
            if (llmResponse == null || llmResponse.trim().isEmpty()) {
                llmResponse = "I found " + products.size() + " product(s) matching your request. Here's the purchase requisition:";
            }
        } catch (Exception e) {
            log.warn("LLM call failed, using default response", e);
            llmResponse = "I found " + products.size() + " product(s) matching your request. Here's the purchase requisition:";
        }
        
        ChatResponse response = new ChatResponse();
        response.setResponse(llmResponse);
        response.setTableData(tableData);
        response.setActionData(actionData);
        
        log.info("Created response with tableData: {}, actionData: {}", 
            tableData != null, actionData != null);
        
        return response;
    }

    private ChatResponse handleSearchProducts(String message) {
        log.info("Handling search products request: {}", message);
        
        // 1. 先进行向量搜索（语义搜索）
        List<ProductMaster> vectorSearchResults = performVectorSearch(message, 10);
        log.info("Vector search found {} similar products", vectorSearchResults.size());
        
        // 2. 使用传统的物料搜索功能（精确匹配）
        MaterialSearchCriteria criteria = llmSearchParser.parseSearchQuery(message);
        log.info("Parsed search criteria: {}", criteria);
        
        List<SalesData> salesDataList = materialSearchService.searchMaterials(criteria);
        log.info("Found {} sales data records from traditional search", salesDataList.size());
        
        // 如果传统搜索无结果，使用 LLM 作为专家重新理解查询并尝试搜索
        if (salesDataList.isEmpty()) {
            log.info("No results from traditional search, asking LLM expert to help find related data");
            
            // 让 LLM 作为建筑领域专家，重新理解用户意图并生成搜索策略
            MaterialSearchCriteria llmCriteria = llmSearchParser.parseSearchQueryWithLLM(message);
            log.info("LLM expert suggested search criteria: {}", llmCriteria);
            
            // 使用 LLM 专家建议的条件再次搜索
            if (!llmCriteria.equals(criteria)) {
                log.info("LLM expert provided different search strategy, trying again");
                salesDataList = materialSearchService.searchMaterials(llmCriteria);
                log.info("LLM expert search found {} results", salesDataList.size());
            }
            
            // 如果传统搜索仍然无结果，但向量搜索有结果，使用向量搜索结果
            if (salesDataList.isEmpty() && !vectorSearchResults.isEmpty()) {
                log.info("Traditional search failed, but vector search found {} products. Using vector search results.", 
                        vectorSearchResults.size());
                // 将向量搜索结果转换为 SalesData 格式
                for (ProductMaster productMaster : vectorSearchResults) {
                    SalesData salesData = convertProductMasterToSalesData(productMaster);
                    salesDataList.add(salesData);
                }
                log.info("Converted {} vector search results to SalesData format", salesDataList.size());
            }
            
            // 如果传统搜索和向量搜索都无结果，让大模型思考一下，提供建议或解释
            if (salesDataList.isEmpty() && vectorSearchResults.isEmpty()) {
                log.info("Both traditional and vector search failed, asking LLM to think about why and provide suggestions");
                
                String llmSuggestion = askLLMForSearchSuggestion(message, criteria, llmCriteria);
                
                ChatResponse response = new ChatResponse();
                
                // 如果大模型提供了建议，使用大模型的回复；否则使用默认消息
                if (llmSuggestion != null && !llmSuggestion.trim().isEmpty()) {
                    response.setResponse(llmSuggestion);
                } else {
                    response.setResponse("Still can't find what you're looking for? Jump to chatbox");
                }
                
                TableData tableData = new TableData();
                tableData.setTitle("Material Search Results");
                tableData.setHeaders(Arrays.asList("id", "Item Code", "Item Name", "Price", "Date", "Category", "Brand", "Function"));
                tableData.setRows(Collections.emptyList());
                tableData.setDescription("Still can't find what you're looking for? Jump to chatbox");
                response.setTableData(tableData);
                
                return response;
            }
        }
        
        // 构建表格数据（包含所有相关字段）
        TableData tableData = new TableData();
        tableData.setTitle("Material Search Results");
        tableData.setHeaders(Arrays.asList(
            "id", "Item Code", "Item Name", "Function", "ItemType", "Model", 
            "Performance", "Performance.1", "Material", "Brand Code", 
            "Bundled", "Origin", "UOM", "TXP1", "TXP2", "Date", "Category"
        ));
        
        List<Map<String, Object>> rows = salesDataList.stream()
            .map(salesData -> {
                Map<String, Object> row = new HashMap<>();
                // 添加 id 字段（主键）
                row.put("id", salesData.getId() != null ? salesData.getId() : "N/A");
                row.put("Item Code", salesData.getItemCode() != null ? salesData.getItemCode() : "N/A");
                row.put("Item Name", salesData.getItemName() != null ? salesData.getItemName() : "N/A");
                row.put("Function", salesData.getFunction() != null ? salesData.getFunction() : "N/A");
                row.put("ItemType", salesData.getItemType() != null ? salesData.getItemType() : "N/A");
                row.put("Model", salesData.getModel() != null ? salesData.getModel() : "N/A");
                row.put("Performance", salesData.getPerformance() != null ? salesData.getPerformance() : "N/A");
                row.put("Performance.1", salesData.getPerformance1() != null ? salesData.getPerformance1() : "N/A");
                row.put("Material", salesData.getMaterial() != null ? salesData.getMaterial() : "N/A");
                row.put("Brand Code", salesData.getBrandCode() != null ? salesData.getBrandCode() : "N/A");
                // 注意：Bundled, Origin, TXP2 字段可能不存在，如果不存在会返回 null
                row.put("Bundled", salesData.getBundled() != null ? salesData.getBundled() : "N/A");
                row.put("Origin", salesData.getOrigin() != null ? salesData.getOrigin() : "N/A");
                row.put("UOM", salesData.getUom() != null ? salesData.getUom() : "N/A");
                row.put("TXP1", salesData.getTxP1() != null ? salesData.getTxP1() : "N/A");
                row.put("TXP2", salesData.getTxP2() != null ? salesData.getTxP2() : "N/A");
                row.put("Date", salesData.getTxDate() != null ? salesData.getTxDate() : "N/A");
                row.put("Category", salesData.getProductHierarchy3() != null ? salesData.getProductHierarchy3() : "N/A");
                return row;
            })
            .collect(Collectors.toList());
        
        tableData.setRows(rows);
        tableData.setDescription("Found " + salesDataList.size() + " material record(s).");
        
        // 3. 合并向量搜索结果和传统搜索结果（如果传统搜索有结果）
        // 将向量搜索结果转换为 SalesData 格式（如果还没有在传统搜索结果中）
        if (!salesDataList.isEmpty() && !vectorSearchResults.isEmpty()) {
            Set<String> existingItemCodes = salesDataList.stream()
                    .map(SalesData::getItemCode)
                    .filter(code -> code != null)
                    .collect(Collectors.toSet());
            
            // 从向量搜索结果中添加新的产品（去重）
            for (ProductMaster productMaster : vectorSearchResults) {
                if (productMaster.getItemCode() != null && 
                    !existingItemCodes.contains(productMaster.getItemCode())) {
                    // 转换为 SalesData 格式（简化版）
                    SalesData salesData = convertProductMasterToSalesData(productMaster);
                    salesDataList.add(salesData);
                }
            }
            log.info("Merged results: {} traditional + {} vector = {} total", 
                    salesDataList.size() - vectorSearchResults.size(), 
                    vectorSearchResults.size(), 
                    salesDataList.size());
        }
        
        // 4. 使用 LLM 基于搜索结果生成智能回答
        String responseText;
        if (criteria.hasItemCode() && salesDataList.size() > 0) {
            MaterialSearchService.MaterialHistoryStats stats = materialSearchService.getMaterialHistory(criteria.getItemCode());
            if (stats != null) {
                responseText = String.format(
                    "Found %d historical transactions for Item Code: %s (%s). " +
                    "Price range: %s - %s, Average: %s. " +
                    "First transaction: %s, Last transaction: %s.",
                    stats.getTotalTransactions(),
                    stats.getItemCode(),
                    stats.getItemName(),
                    stats.getMinPrice() != null ? stats.getMinPrice().toString() : "N/A",
                    stats.getMaxPrice() != null ? stats.getMaxPrice().toString() : "N/A",
                    stats.getAvgPrice() != null ? stats.getAvgPrice().toString() : "N/A",
                    stats.getFirstTransactionDate() != null ? stats.getFirstTransactionDate().toString() : "N/A",
                    stats.getLastTransactionDate() != null ? stats.getLastTransactionDate().toString() : "N/A"
                );
            } else {
                responseText = generateAIResponseFromSearchResults(message, salesDataList, vectorSearchResults);
            }
        } else {
            responseText = generateAIResponseFromSearchResults(message, salesDataList, vectorSearchResults);
        }
        
        ChatResponse response = new ChatResponse();
        response.setResponse(responseText);
        response.setTableData(tableData);
        
        log.info("Returning response with {} records", salesDataList.size());
        return response;
    }

    private ChatResponse handleGetProductInfo(String message) {
        String keyword = extractProductKeyword(message);
        List<Product> products = searchProductsByKeyword(keyword);
        
        if (products.isEmpty()) {
            return createTextResponse("Product not found.");
        }
        
        Product product = products.get(0);
        
        TableData tableData = new TableData();
        tableData.setTitle("Product Information: " + product.getName());
        tableData.setHeaders(Arrays.asList("Field", "Value"));
        
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(createInfoRow("Name", product.getName()));
        rows.add(createInfoRow("Description", product.getDescription()));
        rows.add(createInfoRow("Price", product.getPrice() + " " + product.getCurrency()));
        rows.add(createInfoRow("Stock", product.getStock()));
        rows.add(createInfoRow("Rating", product.getRating()));
        rows.add(createInfoRow("Category", product.getCategory()));
        rows.add(createInfoRow("Seller", product.getSeller() != null ? product.getSeller().getName() : "N/A"));
        if (product.getPeCertified() != null && product.getPeCertified()) {
            rows.add(createInfoRow("PE Certified", "Yes"));
            rows.add(createInfoRow("Certificate Number", product.getCertificateNumber()));
        }
        
        tableData.setRows(rows);
        
        String llmResponse = callLLM(generateProductInfoPrompt(message, product));
        
        ChatResponse response = new ChatResponse();
        response.setResponse(llmResponse);
        response.setTableData(tableData);
        
        return response;
    }

    private ChatResponse handleCompareProducts(String message) {
        // 提取多个产品关键词
        String[] keywords = extractMultipleKeywords(message);
        List<Product> allProducts = new ArrayList<>();
        
        for (String keyword : keywords) {
            allProducts.addAll(searchProductsByKeyword(keyword));
        }
        
        if (allProducts.isEmpty()) {
            return createTextResponse("No products found for comparison.");
        }
        
        TableData tableData = new TableData();
        tableData.setTitle("Product Comparison");
        tableData.setHeaders(Arrays.asList("Product", "Price", "Stock", "Rating", "Category", "Seller"));
        
        List<Map<String, Object>> rows = allProducts.stream()
            .map(product -> {
                Map<String, Object> row = new HashMap<>();
                row.put("Product", product.getName());
                row.put("Price", product.getPrice() + " " + product.getCurrency());
                row.put("Stock", product.getStock());
                row.put("Rating", product.getRating());
                row.put("Category", product.getCategory());
                row.put("Seller", product.getSeller() != null ? product.getSeller().getName() : "N/A");
                return row;
            })
            .collect(Collectors.toList());
        
        tableData.setRows(rows);
        
        String llmResponse = callLLM(generateComparePrompt(message, allProducts));
        
        ChatResponse response = new ChatResponse();
        response.setResponse(llmResponse);
        response.setTableData(tableData);
        
        return response;
    }

    private ChatResponse handleGeneralQuery(String message) {
        // 1. 先进行向量搜索（语义搜索）
        List<ProductMaster> vectorSearchResults = performVectorSearch(message, 5);
        log.info("Vector search found {} similar products for general query", vectorSearchResults.size());
        
        // 2. 尝试提取产品关键词，即使是一般查询
        String productKeyword = extractProductKeyword(message);
        List<Product> products = Collections.emptyList();
        
        if (productKeyword != null && !productKeyword.trim().isEmpty()) {
            products = searchProductsByKeyword(productKeyword);
        }
        
        // 3. 使用 LLM 基于向量搜索结果生成智能回答
        String llmResponse;
        try {
            llmResponse = generateAIResponseFromSearchResults(message, 
                    Collections.emptyList(), vectorSearchResults);
            if (llmResponse == null || llmResponse.trim().isEmpty()) {
                llmResponse = callLLM(generateGeneralPrompt(message));
            }
        } catch (Exception e) {
            log.warn("LLM call failed, using default response", e);
            if (vectorSearchResults.isEmpty()) {
                llmResponse = "I understand your request. How can I help you with our construction materials?";
            } else {
                llmResponse = "I found " + vectorSearchResults.size() + 
                        " related product(s) based on your query. Here are the results:";
            }
        }
        
        ChatResponse response = new ChatResponse();
        response.setResponse(llmResponse);
        
        // 4. 如果找到产品，也返回表格数据（优先使用向量搜索结果）
        if (!vectorSearchResults.isEmpty()) {
            TableData tableData = new TableData();
            tableData.setTitle("Related Products (AI Search)");
            tableData.setHeaders(Arrays.asList("Item Code", "Item Name", "Model", "Material", "Brand", "Category"));
            tableData.setRows(vectorSearchResults.stream()
                .map(pm -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("Item Code", pm.getItemCode() != null ? pm.getItemCode() : "N/A");
                    row.put("Item Name", pm.getItemName() != null ? pm.getItemName() : "N/A");
                    row.put("Model", pm.getModel() != null ? pm.getModel() : "N/A");
                    row.put("Material", pm.getMaterial() != null ? pm.getMaterial() : "N/A");
                    row.put("Brand", pm.getBrandCode() != null ? pm.getBrandCode() : "N/A");
                    row.put("Category", pm.getProductHierarchy3() != null ? pm.getProductHierarchy3() : "N/A");
                    return row;
                })
                .collect(Collectors.toList()));
            tableData.setDescription("Found " + vectorSearchResults.size() + 
                    " related product(s) using AI semantic search.");
            response.setTableData(tableData);
        } else if (!products.isEmpty()) {
            TableData tableData = new TableData();
            tableData.setTitle("Related Products");
            tableData.setHeaders(Arrays.asList("Product Name", "Price", "Stock", "Category"));
            tableData.setRows(products.stream()
                .map(product -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("Product Name", product.getName());
                    row.put("Price", product.getPrice() + " " + product.getCurrency());
                    row.put("Stock", product.getStock());
                    row.put("Category", product.getCategory() != null ? product.getCategory() : "N/A");
                    return row;
                })
                .collect(Collectors.toList()));
            tableData.setDescription("Found " + products.size() + " related product(s).");
            response.setTableData(tableData);
        }
        
        return response;
    }

    private List<Product> searchProductsByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            log.warn("Empty keyword provided for product search");
            return Collections.emptyList();
        }
        
        log.info("Searching products with keyword: {}", keyword);
        
        List<Product> products = new ArrayList<>();
        
        // 1. 先搜索 sales_data 表（ItemName 字段）
        List<SalesData> salesDataList = salesDataRepository.searchByItemName(keyword, 10);
        log.info("Found {} items in sales_data matching keyword: {}", salesDataList.size(), keyword);
        
        // 将 SalesData 转换为 Product（用于统一返回格式）
        for (SalesData salesData : salesDataList) {
            if (salesData.getItemName() != null && !salesData.getItemName().trim().isEmpty()) {
                Product product = convertSalesDataToProduct(salesData);
                products.add(product);
            }
        }
        
        // 2. 如果 sales_data 中没有找到，再搜索 products 表
        if (products.isEmpty()) {
            log.info("No results in sales_data, searching products table");
            products = productRepository.findAll().stream()
                .filter(product -> 
                    (product.getName() != null && product.getName().toLowerCase().contains(keyword.toLowerCase())) ||
                    (product.getDescription() != null && product.getDescription().toLowerCase().contains(keyword.toLowerCase())) ||
                    (product.getCategory() != null && product.getCategory().toLowerCase().contains(keyword.toLowerCase()))
                )
                .limit(10)
                .collect(Collectors.toList());
        }
        
        log.info("Found {} total products matching keyword: {}", products.size(), keyword);
        return products;
    }
    
    /**
     * 将 SalesData 转换为 Product（用于统一返回格式）
     */
    private Product convertSalesDataToProduct(SalesData salesData) {
        Product product = new Product();
        // 使用 ItemName 作为产品名称
        product.setName(salesData.getItemName() != null ? salesData.getItemName() : "Unknown");
        // 使用 TXP1 作为价格
        if (salesData.getTxP1() != null && !salesData.getTxP1().trim().isEmpty()) {
            try {
                product.setPrice(new BigDecimal(salesData.getTxP1()));
            } catch (Exception e) {
                log.warn("Failed to parse price: {}", salesData.getTxP1());
                product.setPrice(BigDecimal.ZERO);
            }
        } else {
            product.setPrice(BigDecimal.ZERO);
        }
        product.setCurrency("USD"); // 默认货币
        // 使用 TXQty 作为库存
        if (salesData.getTxQty() != null && !salesData.getTxQty().trim().isEmpty()) {
            try {
                product.setStock(Integer.parseInt(salesData.getTxQty()));
            } catch (Exception e) {
                log.warn("Failed to parse stock: {}", salesData.getTxQty());
                product.setStock(0);
            }
        } else {
            product.setStock(0);
        }
        // 使用 Product Hierarchy 3 作为分类
        product.setCategory(salesData.getProductHierarchy3());
        // 使用 ItemCode 作为 ID（临时）
        product.setId(salesData.getItemCode() != null ? salesData.getItemCode() : salesData.getTxNo());
        // 设置描述
        StringBuilder description = new StringBuilder();
        if (salesData.getModel() != null) {
            description.append("Model: ").append(salesData.getModel()).append(". ");
        }
        if (salesData.getFunction() != null) {
            description.append("Function: ").append(salesData.getFunction()).append(". ");
        }
        if (salesData.getMaterial() != null) {
            description.append("Material: ").append(salesData.getMaterial()).append(".");
        }
        product.setDescription(description.toString());
        product.setRating(BigDecimal.ZERO);
        
        return product;
    }

    private String extractProductKeyword(String message) {
        // 改进的关键词提取逻辑，能够处理长产品名称
        String[] commonWords = {"create", "a", "purchase", "requisition", "for", "show", "me", "find", "search", "get", "info", "about", "the", "can", "you", "please", "make", "order", "buy"};
        
        // 尝试提取 "for" 或 "requisition for" 之后的内容
        String lowerMessage = message.toLowerCase();
        int forIndex = lowerMessage.indexOf(" for ");
        int requisitionIndex = lowerMessage.indexOf("requisition for");
        
        String keyword = null;
        if (requisitionIndex >= 0) {
            keyword = message.substring(requisitionIndex + "requisition for".length()).trim();
        } else if (forIndex >= 0) {
            keyword = message.substring(forIndex + " for ".length()).trim();
        } else {
            // 如果没有找到 "for"，移除常见词汇
            String[] words = message.split("\\s+");
            List<String> keywords = new ArrayList<>();
            for (String word : words) {
                if (!Arrays.asList(commonWords).contains(word.toLowerCase()) && word.length() > 2) {
                    keywords.add(word);
                }
            }
            keyword = String.join(" ", keywords);
        }
        
        // 清理关键词（移除标点符号等）
        if (keyword != null) {
            keyword = keyword.replaceAll("[.,!?;:]", "").trim();
        }
        
        log.info("Extracted keyword: '{}' from message: '{}'", keyword, message);
        return keyword != null && !keyword.isEmpty() ? keyword : message;
    }
    
    /**
     * 让大模型思考为什么搜索无结果，并提供建议
     */
    private String askLLMForSearchSuggestion(String userQuery, MaterialSearchCriteria initialCriteria, MaterialSearchCriteria llmCriteria) {
        if (llmApiKey == null || llmApiKey.isEmpty()) {
            log.warn("LLM API key not configured, cannot ask LLM for suggestions");
            return null;
        }
        
        try {
            String prompt = buildSearchSuggestionPrompt(userQuery, initialCriteria, llmCriteria);
            log.info("Asking LLM to think about why search returned no results and provide suggestions");
            return callLLM(prompt);
        } catch (Exception e) {
            log.error("Failed to ask LLM for search suggestions", e);
            return null;
        }
    }
    
    /**
     * 构建搜索建议提示词
     */
    private String buildSearchSuggestionPrompt(String userQuery, MaterialSearchCriteria initialCriteria, MaterialSearchCriteria llmCriteria) {
        return "You are a professional expert in construction materials and engineering equipment. The user searched for the following but found no results:\n\n" +
               "User query: \"" + userQuery + "\"\n\n" +
               "Initial search criteria: " + initialCriteria + "\n" +
               "LLM expert suggested search criteria: " + llmCriteria + "\n\n" +
               "Please think about the following questions and provide suggestions:\n" +
               "1. Why might this search have no results?\n" +
               "2. What might the user be trying to search for?\n" +
               "3. Are there similar search terms or synonyms to try?\n" +
               "4. What related categories or brands might be in the database?\n" +
               "5. How should the user modify their query to find results?\n\n" +
               "Please provide a friendly and professional response with specific suggestions. If possible, suggest the user try:\n" +
               "- Using more general keywords\n" +
               "- Checking if spelling is correct\n" +
               "- Trying to search for related categories or brands\n" +
               "- Using partial keywords instead of complete product names\n\n" +
               "Please respond in English only. Keep the response concise and clear, but provide complete information.";
    }

    private String[] extractMultipleKeywords(String message) {
        // 提取多个关键词用于产品比较
        return message.toLowerCase()
            .replaceAll("compare|and|vs|versus", " ")
            .split("\\s+");
    }

    private String callLLM(String prompt) {
        if (llmApiKey == null || llmApiKey.isEmpty()) {
            log.warn("LLM API key not configured, returning default response");
            return "I understand your request. Here's the information you requested.";
        }
        
        try {
            // Google Gemini API 格式: https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={key}
            String apiUrl = llmApiUrl + "/models/" + llmModel + ":generateContent?key=" + llmApiKey;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Google Gemini API 请求格式
            Map<String, Object> requestBody = new HashMap<>();
            List<Map<String, Object>> contents = new ArrayList<>();
            Map<String, Object> content = new HashMap<>();
            content.put("role", "user");
            List<Map<String, Object>> parts = new ArrayList<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", "You are a helpful assistant for a construction materials e-commerce platform. Provide clear, detailed responses in English only. Always respond in English.\n\n" + prompt);
            parts.add(part);
            content.put("parts", parts);
            contents.add(content);
            requestBody.put("contents", contents);
            
            // 生成配置
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 2048); // Increased to allow longer responses
            requestBody.put("generationConfig", generationConfig);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(apiUrl, entity, (Class<Map<String, Object>>) (Class<?>) Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> candidate = candidates.get(0);
                    @SuppressWarnings("unchecked")
                    Map<String, Object> contentResponse = (Map<String, Object>) candidate.get("content");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> partsResponse = (List<Map<String, Object>>) contentResponse.get("parts");
                    if (partsResponse != null && !partsResponse.isEmpty()) {
                        return (String) partsResponse.get(0).get("text");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error calling Google Gemini API", e);
        }
        
        return "I understand your request. Here's the information you requested.";
    }

    private String generateRequisitionPrompt(String userMessage, List<Product> products) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("The user asked: \"").append(userMessage).append("\"\n\n");
        prompt.append("I found the following products:\n");
        for (Product product : products) {
            prompt.append("- ").append(product.getName())
                  .append(" (Price: ").append(product.getPrice()).append(" ")
                  .append(product.getCurrency()).append(", Stock: ").append(product.getStock()).append(")\n");
        }
        prompt.append("\nPlease provide a helpful response confirming the purchase requisition creation.");
        return prompt.toString();
    }

    private String generateSearchPrompt(String userMessage, List<Product> products) {
        return "The user searched for: \"" + userMessage + "\". I found " + products.size() + " product(s). Provide a helpful summary.";
    }

    private String generateProductInfoPrompt(String userMessage, Product product) {
        return "The user asked about: \"" + userMessage + "\". Product details: " + product.getName() + 
               " - " + product.getDescription() + ". Price: " + product.getPrice() + " " + product.getCurrency() + 
               ". Provide a helpful response.";
    }

    private String generateComparePrompt(String userMessage, List<Product> products) {
        StringBuilder prompt = new StringBuilder("The user wants to compare products: \"").append(userMessage).append("\"\n\nProducts:\n");
        for (Product product : products) {
            prompt.append("- ").append(product.getName()).append(": ").append(product.getPrice())
                  .append(" ").append(product.getCurrency()).append("\n");
        }
        prompt.append("\nProvide a comparison summary.");
        return prompt.toString();
    }

    private String generateGeneralPrompt(String userMessage) {
        return "The user asked: \"" + userMessage + "\". Provide a helpful response about construction materials and products.";
    }

    private ChatResponse createTextResponse(String text) {
        ChatResponse response = new ChatResponse();
        response.setResponse(text);
        return response;
    }

    private ChatResponse createErrorResponse(String message) {
        return createTextResponse(message);
    }

    private Map<String, Object> createInfoRow(String field, Object value) {
        Map<String, Object> row = new HashMap<>();
        row.put("Field", field);
        row.put("Value", value != null ? value.toString() : "N/A");
        return row;
    }
    
    /**
     * 执行向量搜索，基于用户查询找到相似的产品
     */
    private List<ProductMaster> performVectorSearch(String query, int topK) {
        try {
            // 1. 为查询生成向量
            List<Float> queryVector = cohereEmbeddingService.generateEmbedding(query);
            if (queryVector == null || queryVector.isEmpty()) {
                log.warn("Failed to generate embedding for query: {}", query);
                return Collections.emptyList();
            }
            
            // 2. 在 Qdrant 中搜索相似向量
            List<Long> similarIds = qdrantService.searchSimilar(queryVector, topK);
            if (similarIds == null || similarIds.isEmpty()) {
                log.info("No similar vectors found for query: {}", query);
                return Collections.emptyList();
            }
            
            // 3. 根据 hash ID 查找 ProductMaster
            // 由于我们使用 product_uid 的 hash 作为 Qdrant ID，需要匹配
            List<ProductMaster> allProducts = productMasterRepository.findWithEmbeddingText();
            List<ProductMaster> results = new ArrayList<>();
            
            // 创建 hash 到 ProductMaster 的映射
            Map<Long, ProductMaster> hashToProductMap = new HashMap<>();
            for (ProductMaster pm : allProducts) {
                if (pm.getProductUid() != null) {
                    long hash = pm.getProductUid().hashCode();
                    hash = hash < 0 ? Math.abs(hash) : hash;
                    hashToProductMap.put(hash, pm);
                }
            }
            
            // 根据返回的 ID 查找对应的产品
            for (Long id : similarIds) {
                ProductMaster pm = hashToProductMap.get(id);
                if (pm != null) {
                    results.add(pm);
                }
            }
            
            log.info("Vector search returned {} products for query: {} (matched {} out of {} IDs)", 
                    results.size(), query, results.size(), similarIds.size());
            return results;
            
        } catch (Exception e) {
            log.error("Error performing vector search for query: {}", query, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 基于搜索结果生成 AI 回答
     */
    private String generateAIResponseFromSearchResults(String userQuery, 
                                                      List<SalesData> salesDataList,
                                                      List<ProductMaster> vectorResults) {
        try {
            // 构建包含搜索结果的提示词
            StringBuilder prompt = new StringBuilder();
            prompt.append("User query: \"").append(userQuery).append("\"\n\n");
            
            if (!vectorResults.isEmpty()) {
                prompt.append("Based on AI semantic search, I found the following related products:\n");
                for (int i = 0; i < Math.min(5, vectorResults.size()); i++) {
                    ProductMaster pm = vectorResults.get(i);
                    prompt.append(String.format("%d. %s", i + 1, pm.getItemName() != null ? pm.getItemName() : "N/A"));
                    if (pm.getModel() != null) {
                        prompt.append(" (Model: ").append(pm.getModel()).append(")");
                    }
                    if (pm.getMaterial() != null) {
                        prompt.append(" - Material: ").append(pm.getMaterial());
                    }
                    prompt.append("\n");
                }
            }
            
            if (!salesDataList.isEmpty()) {
                prompt.append("\nThrough precise search, I also found the following matching products:\n");
                for (int i = 0; i < Math.min(5, salesDataList.size()); i++) {
                    SalesData sd = salesDataList.get(i);
                    prompt.append(String.format("%d. %s", i + 1, sd.getItemName() != null ? sd.getItemName() : "N/A"));
                    if (sd.getModel() != null) {
                        prompt.append(" (Model: ").append(sd.getModel()).append(")");
                    }
                    prompt.append("\n");
                }
            }
            
            prompt.append("\nPlease provide a helpful and professional response to the user based on the search results above.");
            prompt.append("If products were found, briefly introduce these products; if not found, provide suggestions.");
            prompt.append("Keep the response concise and clear, but provide complete information. Use English only.");
            
            String aiResponse = callLLM(prompt.toString());
            if (aiResponse != null && !aiResponse.trim().isEmpty()) {
                return aiResponse;
            }
        } catch (Exception e) {
            log.error("Error generating AI response from search results", e);
        }
        
        // 如果 LLM 调用失败，返回默认回答
        // 注意：vectorResults 中的产品可能已经被转换并添加到 salesDataList 中了
        int totalResults = salesDataList.size();
        if (totalResults > 0) {
            return String.format("I found %d related product(s). Here are the search results:", totalResults);
        } else {
            return "Sorry, I couldn't find any exact matches. Please try different keywords or provide more information about the product you're looking for.";
        }
    }
    
    /**
     * 将 ProductMaster 转换为 SalesData（用于统一返回格式）
     */
    private SalesData convertProductMasterToSalesData(ProductMaster productMaster) {
        SalesData salesData = new SalesData();
        salesData.setItemCode(productMaster.getItemCode());
        salesData.setItemName(productMaster.getItemName());
        salesData.setModel(productMaster.getModel());
        salesData.setMaterial(productMaster.getMaterial());
        salesData.setBrandCode(productMaster.getBrandCode());
        salesData.setUom(productMaster.getUom());
        salesData.setFunction(productMaster.getFunctionName());
        salesData.setItemType(productMaster.getItemType());
        salesData.setProductHierarchy3(productMaster.getProductHierarchy3());
        // 注意：其他字段可能为 null，因为 ProductMaster 和 SalesData 的字段不完全一致
        return salesData;
    }
}

