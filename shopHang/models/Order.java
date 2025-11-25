package com.shopHang.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class Order implements Serializable {
    @SerializedName("_id")
    private String id;
    @SerializedName("customerId")
    private Customer customer; // Có thể là Customer object hoặc null nếu chỉ là ID
    @SerializedName("userId")
    private User user; // Có thể là User object hoặc null nếu chỉ là ID
    private List<OrderItem> items;
    @SerializedName("totalAmount")
    private double totalAmount;
    @SerializedName("discountAmount")
    private double discountAmount;
    @SerializedName("shippingFee")
    private double shippingFee;
    @SerializedName("finalAmount")
    private double finalAmount;
    @SerializedName("voucherCode")
    private String voucherCode;
    private String status;
    private String createdAt;
    
    public Order() {
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public Customer getCustomer() {
        return customer;
    }
    
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public List<OrderItem> getItems() {
        return items;
    }
    
    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
    
    public double getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public double getDiscountAmount() {
        return discountAmount;
    }
    
    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }
    
    public double getFinalAmount() {
        return finalAmount > 0 ? finalAmount : totalAmount;
    }
    
    public void setFinalAmount(double finalAmount) {
        this.finalAmount = finalAmount;
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
    
    public String getFormattedShippingFee() {
        return String.format("%,.0f₫", shippingFee);
    }
    
    public String getFormattedTotal() {
        // Hiển thị finalAmount nếu có, nếu không thì hiển thị totalAmount
        double amount = getFinalAmount();
        return String.format("%,.0f₫", amount);
    }
    
    public String getFormattedSubtotal() {
        return String.format("%,.0f₫", totalAmount);
    }
    
    public String getFormattedDiscount() {
        if (discountAmount > 0) {
            return String.format("%,.0f₫", discountAmount);
        }
        return "0₫";
    }
    
    public String getStatusText() {
        if (status == null) return "N/A";
        switch (status) {
            case "pending": return "Chờ xử lý";
            case "processing": return "Đang xử lý";
            case "delivering": return "Đang giao hàng";
            case "shipped": return "Đã giao hàng";
            case "paid": return "Đã thanh toán";
            case "cancelled": return "Đã hủy";
            default: return status;
        }
    }
    
    public static class OrderItem implements Serializable {
        @SerializedName("productId")
        private Product product; // Có thể là Product object hoặc null nếu chỉ là ID
        private int quantity;
        private double price;
        
        public Product getProduct() {
            return product;
        }
        
        public void setProduct(Product product) {
            this.product = product;
        }
        
        public int getQuantity() {
            return quantity;
        }
        
        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
        
        public double getPrice() {
            return price;
        }
        
        public void setPrice(double price) {
            this.price = price;
        }
        
        public double getSubtotal() {
            return price * quantity;
        }
    }
}

