package com.ecosystem.service;

import com.ecosystem.dto.punchout.CxmlSetupRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * cXML解析服务
 * 实现严格的协议骨架校验和宽松的业务字段处理
 */
@Service
@Slf4j
public class CxmlParserService {

    /**
     * 解析cXML格式的PunchoutSetupRequest
     * 
     * @param xmlContent XML内容字符串
     * @return CxmlSetupRequest对象
     * @throws Exception 解析失败时抛出异常
     */
    public CxmlSetupRequest parseSetupRequest(String xmlContent) throws Exception {
        // 空值保护：在最开始就检查
        if (xmlContent == null || xmlContent.isBlank()) {
            throw new IllegalArgumentException("xmlContent is empty or null");
        }
        
        // 提取payloadID用于错误日志
        String payloadId = extractPayloadId(xmlContent);
        
        // 日志脱敏：移除SharedSecret等敏感信息（保证不返回null）
        String sanitizedXml = sanitizeXmlForLogging(xmlContent);
        if (payloadId != null) {
            log.debug("Parsing cXML content (payloadID: {}): {}", payloadId, 
                sanitizedXml.length() > 500 ? sanitizedXml.substring(0, 500) + "..." : sanitizedXml);
        } else {
            log.debug("Parsing cXML content: {}", 
                sanitizedXml.length() > 500 ? sanitizedXml.substring(0, 500) + "..." : sanitizedXml);
        }
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        
        // 安全加固：允许DOCTYPE存在（Coupa的cXML需要），但禁止加载外部DTD/实体，防止XXE攻击
        // 注意：不设置disallow-doctype-decl=true，因为Coupa的请求包含DOCTYPE声明
        try {
            // 禁止外部通用实体
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            // 禁止外部参数实体
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            // 禁止加载外部DTD
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            // 禁用XInclude
            factory.setXIncludeAware(false);
            // 禁用实体引用展开
            factory.setExpandEntityReferences(false);
        } catch (Exception e) {
            log.warn("Failed to set some XML security features: {}", e.getMessage());
        }
        
        DocumentBuilder builder = factory.newDocumentBuilder();
        
        InputStream inputStream = new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8));
        Document document;
        try {
            document = builder.parse(inputStream);
            document.getDocumentElement().normalize();
        } catch (Exception e) {
            // 解析失败时，记录payloadID和简要信息
            String errorMsg = String.format("Failed to parse cXML (payloadID: %s): %s", 
                payloadId != null ? payloadId : "unknown", e.getMessage());
            log.warn("{} - XML preview: {}", errorMsg, 
                sanitizedXml.length() > 200 ? sanitizedXml.substring(0, 200) + "..." : sanitizedXml);
            throw new IllegalArgumentException(errorMsg, e);
        }
        
        CxmlSetupRequest request = new CxmlSetupRequest();
        
        // 解析Header部分
        parseHeader(document, request);
        
        // 解析Request部分（协议骨架 - 严格校验）
        parseRequest(document, request);
        
        // 解析业务字段（宽松处理）
        parseBusinessFields(document, request);
        
        log.info("Parsed cXML request: buyerCookie={}, operation={}, senderIdentity={}", 
            request.getBuyerCookie(), request.getOperation(), request.getSenderIdentity());
        
        return request;
    }
    
    /**
     * 解析Header部分（协议骨架 - 必须）
     */
    private void parseHeader(Document document, CxmlSetupRequest request) {
        // 使用精确查找，支持namespace前缀
        Element root = document.getDocumentElement();
        Element header = getDirectChildElement(root, "Header");
        if (header == null) {
            // Header是协议骨架的一部分，缺失时直接抛异常
            throw new IllegalArgumentException("Header element is required in cXML (protocol skeleton)");
        }
        
        // 解析From - 先定位到Header的直接子节点From
        Element from = getDirectChildElement(header, "From");
        if (from != null) {
            Element credential = getDirectChildElement(from, "Credential");
            if (credential != null) {
                request.setFromDomain(credential.getAttribute("domain"));
                request.setFromIdentity(getDirectChildTextContent(credential, "Identity"));
            }
        }
        
        // 解析To - 先定位到Header的直接子节点To
        Element to = getDirectChildElement(header, "To");
        if (to != null) {
            Element credential = getDirectChildElement(to, "Credential");
            if (credential != null) {
                request.setToDomain(credential.getAttribute("domain"));
                request.setToIdentity(getDirectChildTextContent(credential, "Identity"));
            }
        }
        
        // 解析Sender（协议骨架 - 必须）- 先定位到Header的直接子节点Sender
        Element sender = getDirectChildElement(header, "Sender");
        if (sender == null) {
            throw new IllegalArgumentException("Sender element is required in Header (protocol skeleton)");
        }
        
        Element credential = getDirectChildElement(sender, "Credential");
        if (credential == null) {
            throw new IllegalArgumentException("Sender.Credential is required in Header (protocol skeleton)");
        }
        
        String senderIdentity = getDirectChildTextContent(credential, "Identity");
        String senderSharedSecret = getDirectChildTextContent(credential, "SharedSecret");
        
        // 严格校验：Sender.Identity和SharedSecret是必须的
        if (senderIdentity == null || senderIdentity.trim().isEmpty()) {
            throw new IllegalArgumentException("Sender.Credential.Identity is required in Header (protocol skeleton)");
        }
        if (senderSharedSecret == null || senderSharedSecret.trim().isEmpty()) {
            throw new IllegalArgumentException("Sender.Credential.SharedSecret is required in Header (protocol skeleton)");
        }
        
        request.setSenderDomain(credential.getAttribute("domain"));
        request.setSenderIdentity(senderIdentity.trim());
        request.setSenderSharedSecret(senderSharedSecret.trim());
        
        // UserAgent（可选）- Sender的直接子节点
        request.setUserAgent(getDirectChildTextContent(sender, "UserAgent"));
    }
    
    /**
     * 解析Request部分（协议骨架 - 严格校验）
     */
    private void parseRequest(Document document, CxmlSetupRequest request) {
        // 先找到根元素下的Request（更精确）
        Element root = document.getDocumentElement();
        Element requestElement = getDirectChildElement(root, "Request");
        if (requestElement == null) {
            throw new IllegalArgumentException("Request element is required in cXML");
        }
        
        // 使用getDirectChildElement更精确，避免全树搜索
        Element punchoutSetup = getDirectChildElement(requestElement, "PunchOutSetupRequest");
        if (punchoutSetup == null) {
            throw new IllegalArgumentException("PunchOutSetupRequest element is required in Request");
        }
        
        // Operation（可选，默认为"create"）
        String operation = punchoutSetup.getAttribute("operation");
        request.setOperation((operation == null || operation.isBlank()) ? "create" : operation.trim());
        
        // BuyerCookie - 必须字段（严格校验）
        // 使用精确查找，避免全树搜索取到不期望的同名节点
        String buyerCookie = getDirectChildTextContent(punchoutSetup, "BuyerCookie");
        if (buyerCookie == null || buyerCookie.trim().isEmpty()) {
            throw new IllegalArgumentException("BuyerCookie is required in PunchOutSetupRequest");
        }
        request.setBuyerCookie(buyerCookie.trim());
        
        // BrowserFormPost.URL - 必须字段（严格校验）
        // 先定位到PunchOutSetupRequest的直接子节点BrowserFormPost
        Element browserFormPost = getDirectChildElement(punchoutSetup, "BrowserFormPost");
        if (browserFormPost != null) {
            // 在BrowserFormPost内获取URL（直接子节点）
            String url = getDirectChildTextContent(browserFormPost, "URL");
            if (url == null || url.trim().isEmpty()) {
                throw new IllegalArgumentException("BrowserFormPost.URL is required in PunchOutSetupRequest");
            }
            request.setBrowserFormPostUrl(url.trim());
        } else {
            throw new IllegalArgumentException("BrowserFormPost is required in PunchOutSetupRequest");
        }
    }
    
    /**
     * 解析业务字段（宽松处理）
     */
    private void parseBusinessFields(Document document, CxmlSetupRequest request) {
        Element documentRoot = document.getDocumentElement();
        Element requestElement = getDirectChildElement(documentRoot, "Request");
        if (requestElement == null) {
            return;
        }
        
        Element punchoutSetup = getDirectChildElement(requestElement, "PunchOutSetupRequest");
        if (punchoutSetup == null) {
            return;
        }
        
        // 解析Extrinsic字段（宽松处理 - 能用就用）
        // 只取PunchOutSetupRequest的直接子节点Extrinsic，避免全树搜索
        Map<String, String> extrinsicMap = new HashMap<>();
        NodeList children = punchoutSetup.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = getNodeLocalName(child);
                if ("Extrinsic".equals(nodeName)) {
                    Element extrinsic = (Element) child;
                    String name = extrinsic.getAttribute("name");
                    String value = extrinsic.getTextContent();
                    if (name != null && !name.isEmpty() && value != null) {
                        extrinsicMap.put(name, value.trim());
                    }
                }
            }
        }
        request.setExtrinsic(extrinsicMap);
        
        // 解析Contact（可选）- 先定位到PunchOutSetupRequest的直接子节点Contact
        Element contact = getDirectChildElement(punchoutSetup, "Contact");
        if (contact != null) {
            CxmlSetupRequest.ContactInfo contactInfo = new CxmlSetupRequest.ContactInfo();
            contactInfo.setRole(contact.getAttribute("role"));
            // 在Contact内获取Name和Email（直接子节点）
            contactInfo.setName(getDirectChildTextContent(contact, "Name"));
            contactInfo.setEmail(getDirectChildTextContent(contact, "Email"));
            request.setContact(contactInfo);
        }
        
        // 解析ShipTo（可选）
        // 正确结构：<ShipTo><Address addressID="..."><Name>...</Name><PostalAddress><Street>...</Street>...</PostalAddress></Address></ShipTo>
        // 先定位到PunchOutSetupRequest的直接子节点ShipTo
        Element shipTo = getDirectChildElement(punchoutSetup, "ShipTo");
        if (shipTo != null) {
            // 在ShipTo内查找Address节点（直接子节点）
            Element address = getDirectChildElement(shipTo, "Address");
            if (address != null) {
                // 只有当Address存在时才创建ShipToInfo
                CxmlSetupRequest.ShipToInfo shipToInfo = new CxmlSetupRequest.ShipToInfo();
                
                // addressID在Address节点的属性上
                shipToInfo.setAddressId(address.getAttribute("addressID"));
                
                // Name在Address的直接子节点
                shipToInfo.setName(getDirectChildTextContent(address, "Name"));
                
                // 在Address内查找PostalAddress（直接子节点）
                Element postalAddress = getDirectChildElement(address, "PostalAddress");
                if (postalAddress != null) {
                    // Street, City, PostalCode, Country在PostalAddress的直接子节点
                    shipToInfo.setStreet(getDirectChildTextContent(postalAddress, "Street"));
                    shipToInfo.setCity(getDirectChildTextContent(postalAddress, "City"));
                    shipToInfo.setPostalCode(getDirectChildTextContent(postalAddress, "PostalCode"));
                    shipToInfo.setCountry(getDirectChildTextContent(postalAddress, "Country"));
                }
                
                // 只有当Address存在时才set ShipToInfo
                request.setShipTo(shipToInfo);
            }
            // 如果ShipTo存在但没有Address，不设置shipToInfo（保持为null）
        }
        
        // 解析cXML根元素属性（可选）
        request.setPayloadId(documentRoot.getAttribute("payloadID"));
        request.setTimestamp(documentRoot.getAttribute("timestamp"));
    }
    
    /**
     * 获取节点的本地名称（处理namespace前缀）
     * 如果节点有namespace前缀（如cXML:Header），返回Header
     * 如果没有前缀，返回节点名称
     */
    private String getNodeLocalName(Node node) {
        String localName = node.getLocalName();
        return (localName != null && !localName.isEmpty()) ? localName : node.getNodeName();
    }
    
    /**
     * 获取直接子元素的文本内容（精确查找，避免取到不期望的同名节点）
     * 只查找parent的直接子节点，不递归搜索
     * 支持namespace前缀（使用getLocalName）
     */
    private String getDirectChildTextContent(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = getNodeLocalName(child);
                if (nodeName.equals(tagName)) {
                    return child.getTextContent();
                }
            }
        }
        return null;
    }
    
    /**
     * 获取直接子元素（精确查找）
     * 支持namespace前缀（使用getLocalName）
     */
    private Element getDirectChildElement(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = getNodeLocalName(child);
                if (nodeName.equals(tagName)) {
                    return (Element) child;
                }
            }
        }
        return null;
    }
    
    /**
     * XML日志脱敏：移除敏感信息（如SharedSecret）
     * 使用DOTALL模式支持跨行匹配
     * 保证不返回null，返回空字符串
     */
    private String sanitizeXmlForLogging(String xmlContent) {
        if (xmlContent == null) {
            return "";
        }
        // 替换SharedSecret标签内容为***（使用DOTALL模式支持跨行）
        return xmlContent.replaceAll("(?is)<SharedSecret>.*?</SharedSecret>", "<SharedSecret>***</SharedSecret>");
    }
    
    /**
     * 从XML字符串中提取payloadID（用于错误日志）
     */
    private String extractPayloadId(String xmlContent) {
        if (xmlContent == null) {
            return null;
        }
        try {
            // 尝试从cXML根元素提取payloadID属性
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
    
}

