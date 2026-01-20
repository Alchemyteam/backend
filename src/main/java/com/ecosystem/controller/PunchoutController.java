package com.ecosystem.controller;

import com.ecosystem.dto.ErrorResponse;
import com.ecosystem.dto.punchout.*;
import com.ecosystem.service.CxmlParserService;
import com.ecosystem.service.PunchoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Punchout Controller - 处理punchout相关的API请求
 */
@RestController
@RequestMapping("/punchout")
@RequiredArgsConstructor
@Slf4j
public class PunchoutController {

    private final PunchoutService punchoutService;
    private final CxmlParserService cxmlParserService;

    /**
     * Punchout Setup端点 - 支持cXML格式
     * 接收来自采购系统的punchout设置请求（cXML格式），验证用户，生成会话token，返回cXML响应
     * 
     * POST /api/punchout/setup
     * Content-Type: application/xml 或 text/xml
     */
    @PostMapping(value = "/setup", consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE}, 
                 produces = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
    public ResponseEntity<String> setupPunchoutCxml(@RequestBody String xmlContent) {
        String payloadId = extractPayloadId(xmlContent);
        String logPrefix = payloadId != null ? String.format("[payloadID: %s] ", payloadId) : "";
        
        try {
            log.info("{}Received cXML punchout setup request", logPrefix);
            
            // 解析cXML
            CxmlSetupRequest cxmlRequest = cxmlParserService.parseSetupRequest(xmlContent);
            
            // 处理punchout setup
            String cxmlResponse = punchoutService.setupPunchoutCxml(cxmlRequest);
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(cxmlResponse);
            
        } catch (IllegalArgumentException e) {
            log.error("{}Invalid cXML request: {}", logPrefix, e.getMessage());
            // 返回cXML格式的错误响应（HTTP 200 + cXML Status 400）
            // 很多采购系统更喜欢总是收到HTTP 200，通过cXML Status判断成功/失败
            String errorResponse = generateCxmlErrorResponse("400", "Bad Request", 
                sanitizeErrorMessage(e.getMessage()), payloadId);
            return ResponseEntity.ok()  // HTTP 200，错误通过cXML Status表达
                .contentType(MediaType.APPLICATION_XML)
                .body(errorResponse);
        } catch (RuntimeException e) {
            log.error("{}Error processing cXML punchout setup: {}", logPrefix, e.getMessage(), e);
            // 根据错误类型判断状态码
            String statusCode = "500";
            String statusText = "Internal Server Error";
            if (e.getMessage() != null && e.getMessage().contains("Authentication failed")) {
                statusCode = "401";
                statusText = "Unauthorized";
            } else if (e.getMessage() != null && e.getMessage().contains("not found")) {
                statusCode = "404";
                statusText = "Not Found";
            }
            String errorResponse = generateCxmlErrorResponse(statusCode, statusText, 
                sanitizeErrorMessage(e.getMessage()), payloadId);
            return ResponseEntity.ok()  // HTTP 200，错误通过cXML Status表达
                .contentType(MediaType.APPLICATION_XML)
                .body(errorResponse);
        } catch (Exception e) {
            log.error("{}Unexpected error processing cXML punchout setup: {}", logPrefix, e.getMessage(), e);
            String errorResponse = generateCxmlErrorResponse("500", "Internal Server Error", 
                "An unexpected error occurred", payloadId);
            return ResponseEntity.ok()  // HTTP 200，错误通过cXML Status表达
                .contentType(MediaType.APPLICATION_XML)
                .body(errorResponse);
        }
    }
    
    /**
     * 从XML字符串中提取payloadID
     */
    private String extractPayloadId(String xmlContent) {
        if (xmlContent == null) {
            return null;
        }
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "payloadID\\s*=\\s*[\"']([^\"']+)[\"']", 
                java.util.regex.Pattern.CASE_INSENSITIVE
            );
            java.util.regex.Matcher matcher = pattern.matcher(xmlContent);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            // 忽略提取失败
        }
        return null;
    }
    
    /**
     * 清理错误消息，移除可能包含的敏感信息
     */
    private String sanitizeErrorMessage(String message) {
        if (message == null) {
            return "Unknown error";
        }
        // 移除可能包含的完整XML内容
        if (message.length() > 500) {
            return message.substring(0, 500) + "...";
        }
        return message;
    }
    
    /**
     * Punchout Setup端点 - JSON格式（向后兼容）
     * 接收来自采购系统的punchout设置请求，验证用户，生成会话token，返回重定向URL
     * 
     * POST /api/punchout/setup
     * Content-Type: application/json
     */
    @PostMapping(value = "/setup", consumes = MediaType.APPLICATION_JSON_VALUE, 
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> setupPunchoutJson(@Valid @RequestBody PunchoutSetupRequest request) {
        try {
            log.info("Received punchout setup request: buyerCookie={}, buyerId={}", 
                request.getBuyerCookie(), request.getBuyerId());
            
            PunchoutSetupResponse response = punchoutService.setupPunchout(request);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Error processing punchout setup: ", e);
            ErrorResponse errorResponse = new ErrorResponse(
                "Failed to setup punchout session",
                null,
                e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        } catch (Exception e) {
            log.error("Unexpected error processing punchout setup: ", e);
            ErrorResponse errorResponse = new ErrorResponse(
                "Internal server error",
                null,
                "An unexpected error occurred"
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Punchout Return端点
     * 接收从购物网站返回的购物车数据，创建订单
     * 
     * POST /api/punchout/return
     */
    @PostMapping("/return")
    public ResponseEntity<?> returnPunchout(@Valid @RequestBody PunchoutReturnRequest request) {
        try {
            log.info("Received punchout return request: sessionToken={}, itemsCount={}", 
                request.getSessionToken(), 
                request.getItems() != null ? request.getItems().size() : 0);
            
            PunchoutReturnResponse response = punchoutService.returnPunchout(request);
            
            if (response.getSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                ErrorResponse errorResponse = new ErrorResponse(
                    "Failed to process punchout return",
                    null,
                    response.getMessage()
                );
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }
            
        } catch (Exception e) {
            log.error("Error processing punchout return: ", e);
            ErrorResponse errorResponse = new ErrorResponse(
                "Internal server error",
                null,
                "An unexpected error occurred: " + e.getMessage()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 验证会话token（可选，用于前端验证）
     * 
     * GET /api/punchout/validate?sessionToken=xxx
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateSession(@RequestParam String sessionToken) {
        try {
            PunchoutService.SessionValidationResult result = punchoutService.validateSession(sessionToken);
            Map<String, Object> response = new HashMap<>();
            response.put("valid", result.isValid());
            response.put("message", result.getMessage());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error validating session: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("message", "Error validating session: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 生成cXML格式的错误响应
     * 
     * @param code HTTP状态码（转换为cXML状态码）
     * @param text 状态文本
     * @param message 错误消息
     * @param requestPayloadId 原始请求的payloadID（如果可用）
     */
    private String generateCxmlErrorResponse(String code, String text, String message, String requestPayloadId) {
        // 如果有原始payloadID，使用它；否则生成新的
        String payloadId = requestPayloadId != null ? requestPayloadId : 
            ("error-" + System.currentTimeMillis() + "@" + getHostname());
        String timestamp = java.time.OffsetDateTime.now().toString();
        
        // 构建错误消息（如果message不为空）
        String errorMessageXml = "";
        if (message != null && !message.trim().isEmpty()) {
            errorMessageXml = "    <Message>" + escapeXml(message) + "</Message>\n";
        }
        
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<!DOCTYPE cXML SYSTEM \"http://xml.cxml.org/schemas/cXML/1.2.014/cXML.dtd\">\n" +
               "<cXML xml:lang=\"en-US\" payloadID=\"" + escapeXml(payloadId) + "\" timestamp=\"" + escapeXml(timestamp) + "\">\n" +
               "  <Response>\n" +
               "    <Status code=\"" + escapeXml(code) + "\" text=\"" + escapeXml(text) + "\"/>\n" +
               errorMessageXml +
               "    <PunchOutSetupResponse>\n" +
               "      <StartPage>\n" +
               "        <URL></URL>\n" +
               "      </StartPage>\n" +
               "    </PunchOutSetupResponse>\n" +
               "  </Response>\n" +
               "</cXML>";
    }
    
    private String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
    
    private String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "localhost";
        }
    }
}

