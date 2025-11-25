package com.shopHang.models;

import java.util.List;

public class CreateOrderRequest {
    private String customerId;
    private List<OrderItemRequest> items;
    private String paymentMethod;
    private String voucherCode;
    private double shippingFee;
    
    public CreateOrderRequest(String customerId, List<OrderItemRequest> items) {
        this.customerId = customerId;
        this.items = items;
        this.shippingFee = 30000; // Mặc định 30,000₫
    }
    
    public CreateOrderRequest(String customerId, List<OrderItemRequest> items, String paymentMethod) {
        this.customerId = customerId;
        this.items = items;
        this.paymentMethod = paymentMethod;
        this.shippingFee = 30000; // Mặc định 30,000₫
    }
    
    public CreateOrderRequest(String customerId, List<OrderItemRequest> items, String paymentMethod, String voucherCode) {
        this.customerId = customerId;
        this.items = items;
        this.paymentMethod = paymentMethod;
        this.voucherCode = voucherCode;
        this.shippingFee = 30000; // Mặc định 30,000₫
    }
    
    public CreateOrderRequest(String customerId, List<OrderItemRequest> items, String paymentMethod, String voucherCode, double shippingFee) {
        this.customerId = customerId;
        this.items = items;
        this.paymentMethod = paymentMethod;
        this.voucherCode = voucherCode;
        this.shippingFee = shippingFee;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public List<OrderItemRequest> getItems() {
        return items;
    }
    
    public void setItems(List<OrderItemRequest> items) {
        this.items = items;
    }
    
    public String getPaymentMethod() {
        return paymentMethod;
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
    
    public String getVoucherCode() {
        return voucherCode;
    }
    
    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }
    
    public double getShippingFee() {
        return shippingFee;
    }
    
    public void setShippingFee(double shippingFee) {
        this.shippingFee = shippingFee;
    }
    
    public static class OrderItemRequest {
        private String productId;
        private int quantity;
        
        public OrderItemRequest(String productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
        
        public String getProductId() {
            return productId;
        }
        
        public void setProductId(String productId) {
            this.productId = productId;
        }
        
        public int getQuantity() {
            return quantity;
        }
        
        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }
}

