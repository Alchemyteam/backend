package com.ecosystem.service;

import com.ecosystem.dto.punchout.PunchoutCartItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * PunchOutOrderMessage cXML Builder
 * 构建标准的 cXML 格式，用于将购物车返回给采购系统
 */
@Component
@Slf4j
public class PunchOutOrderMessageBuilder {

    /**
     * 构建 PunchOutOrderMessage cXML
     * 
     * 注意：
     * - From = Supplier (供应商，发送方)
     * - To = Buyer (买方，接收方)
     * - Sender = Supplier (供应商，不包含SharedSecret)
     * 
     * @param buyerCookie 买方Cookie
     * @param supplierIdentity 供应商标识（From）
     * @param supplierDomain 供应商域名（From）
     * @param buyerIdentity 买方标识（To）
     * @param buyerDomain 买方域名（To）
     * @param senderIdentity 发送者标识（Sender，通常与Supplier相同）
     * @param items 购物车项列表
     * @return cXML格式的字符串
     */
    public String buildPunchOutOrderMessage(
            String buyerCookie,
            String supplierIdentity,
            String supplierDomain,
            String buyerIdentity,
            String buyerDomain,
            String senderIdentity,
            List<PunchoutCartItem> items) {
        
        String payloadId = "punchout-order-" + System.currentTimeMillis() + "@" + getHostname();
        String timestamp = OffsetDateTime.now().toString();
        
        // 计算总金额
        BigDecimal total = BigDecimal.ZERO;
        for (PunchoutCartItem item : items) {
            if (item.getUnitPrice() != null && item.getQuantity() != null) {
                total = total.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }
        
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<!DOCTYPE cXML SYSTEM \"http://xml.cxml.org/schemas/cXML/1.2.014/cXML.dtd\">\n");
        xml.append("<cXML xml:lang=\"en-US\" payloadID=\"").append(escapeXml(payloadId))
           .append("\" timestamp=\"").append(escapeXml(timestamp)).append("\">\n");
        
        // Header
        // From = Supplier (供应商，发送方)
        // To = Buyer (买方，接收方)
        // Sender = Supplier (供应商，不包含SharedSecret)
        xml.append("  <Header>\n");
        xml.append("    <From>\n");
        xml.append("      <Credential domain=\"").append(escapeXml(supplierDomain != null ? supplierDomain : "allinton.com.sg")).append("\">\n");
        xml.append("        <Identity>").append(escapeXml(supplierIdentity != null ? supplierIdentity : "Allinton123")).append("</Identity>\n");
        xml.append("      </Credential>\n");
        xml.append("    </From>\n");
        xml.append("    <To>\n");
        xml.append("      <Credential domain=\"").append(escapeXml(buyerDomain != null ? buyerDomain : "NetworkID")).append("\">\n");
        xml.append("        <Identity>").append(escapeXml(buyerIdentity != null ? buyerIdentity : "AIR_LIQUIDE")).append("</Identity>\n");
        xml.append("      </Credential>\n");
        xml.append("    </To>\n");
        xml.append("    <Sender>\n");
        xml.append("      <Credential domain=\"").append(escapeXml(supplierDomain != null ? supplierDomain : "allinton.com.sg")).append("\">\n");
        xml.append("        <Identity>").append(escapeXml(senderIdentity != null ? senderIdentity : supplierIdentity)).append("</Identity>\n");
        // 注意：不包含SharedSecret（SharedSecret只用于认证，不用于业务消息）
        xml.append("      </Credential>\n");
        xml.append("      <UserAgent>Ecosystem Backend</UserAgent>\n");
        xml.append("    </Sender>\n");
        xml.append("  </Header>\n");
        
        // Request
        xml.append("  <Request deploymentMode=\"production\">\n");
        xml.append("    <PunchOutOrderMessage>\n");
        xml.append("      <BuyerCookie>").append(escapeXml(buyerCookie)).append("</BuyerCookie>\n");
        
        // PunchOutOrderMessageHeader
        xml.append("      <PunchOutOrderMessageHeader>\n");
        xml.append("        <Total>\n");
        xml.append("          <Money currency=\"USD\">").append(total).append("</Money>\n");
        xml.append("        </Total>\n");
        xml.append("      </PunchOutOrderMessageHeader>\n");
        
        // Items
        for (PunchoutCartItem item : items) {
            xml.append("      <ItemIn quantity=\"").append(item.getQuantity() != null ? item.getQuantity() : 1).append("\">\n");
            
            // ItemID
            xml.append("        <ItemID>\n");
            if (item.getSupplierPartId() != null && !item.getSupplierPartId().isEmpty()) {
                xml.append("          <SupplierPartID>").append(escapeXml(item.getSupplierPartId())).append("</SupplierPartID>\n");
            } else if (item.getSku() != null && !item.getSku().isEmpty()) {
                xml.append("          <SupplierPartID>").append(escapeXml(item.getSku())).append("</SupplierPartID>\n");
            } else if (item.getProductId() != null && !item.getProductId().isEmpty()) {
                xml.append("          <SupplierPartID>").append(escapeXml(item.getProductId())).append("</SupplierPartID>\n");
            }
            xml.append("        </ItemID>\n");
            
            // ItemDetail
            xml.append("        <ItemDetail>\n");
            if (item.getUnitPrice() != null) {
                xml.append("          <UnitPrice>\n");
                xml.append("            <Money currency=\"USD\">").append(item.getUnitPrice()).append("</Money>\n");
                xml.append("          </UnitPrice>\n");
            }
            if (item.getProductName() != null && !item.getProductName().isEmpty()) {
                xml.append("          <Description xml:lang=\"en\">").append(escapeXml(item.getProductName())).append("</Description>\n");
            }
            if (item.getUnitOfMeasure() != null && !item.getUnitOfMeasure().isEmpty()) {
                xml.append("          <UnitOfMeasure>").append(escapeXml(item.getUnitOfMeasure())).append("</UnitOfMeasure>\n");
            }
            xml.append("        </ItemDetail>\n");
            
            xml.append("      </ItemIn>\n");
        }
        
        xml.append("    </PunchOutOrderMessage>\n");
        xml.append("  </Request>\n");
        xml.append("</cXML>");
        
        return xml.toString();
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

