package com.shopHang.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Review implements Serializable {
    @SerializedName("_id")
    private String id;
    @SerializedName("productId")
    private Product product;
    @SerializedName("customerId")
    private Customer customer;
    private int rating;
    private String comment;
    private String createdAt;
    @SerializedName("adminReply")
    private String adminReply;
    @SerializedName("adminRepliedAt")
    private String adminRepliedAt;
    @SerializedName("orderId")
    private Order order;
    
    public Review() {
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public Product getProduct() {
        return product;
    }
    
    public void setProduct(Product product) {
        this.product = product;
    }
    
    public Customer getCustomer() {
        return customer;
    }
    
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
    
    public int getRating() {
        return rating;
    }
    
    public void setRating(int rating) {
        this.rating = rating;
    }
    
    public String getComment() {
        return comment;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getRatingStars() {
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < rating; i++) {
            stars.append("★");
        }
        for (int i = rating; i < 5; i++) {
            stars.append("☆");
        }
        return stars.toString();
    }
    
    public String getAdminReply() {
        return adminReply;
    }
    
    public void setAdminReply(String adminReply) {
        this.adminReply = adminReply;
    }
    
    public String getAdminRepliedAt() {
        return adminRepliedAt;
    }
    
    public void setAdminRepliedAt(String adminRepliedAt) {
        this.adminRepliedAt = adminRepliedAt;
    }
    
    public boolean hasAdminReply() {
        return adminReply != null && !adminReply.trim().isEmpty();
    }
    
    public Order getOrder() {
        return order;
    }
    
    public void setOrder(Order order) {
        this.order = order;
    }
}

