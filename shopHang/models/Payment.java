package com.shopHang.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Payment implements Serializable {
    @SerializedName("_id")
    private String id;
    @SerializedName("orderId")
    private Order order;
    private double amount;
    private String method;
    private String status;
    private String paidAt;
    private String createdAt;
    
    public Payment() {
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public Order getOrder() {
        return order;
    }
    
    public void setOrder(Order order) {
        this.order = order;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getPaidAt() {
        return paidAt;
    }
    
    public void setPaidAt(String paidAt) {
        this.paidAt = paidAt;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getFormattedAmount() {
        return String.format("%,.0f₫", amount);
    }
    
    public String getStatusText() {
        switch (status) {
            case "pending": return "Chờ xử lý";
            case "success": return "Thành công";
            case "failed": return "Thất bại";
            default: return status;
        }
    }
    
    public String getMethodText() {
        if (method == null) return "N/A";
        switch (method) {
            case "cash": return "Thanh toán khi nhận hàng (COD)";
            case "card": return "Chuyển khoản ngân hàng";
            case "online": return "Ví điện tử MoMo";
            default: return method;
        }
    }
}

