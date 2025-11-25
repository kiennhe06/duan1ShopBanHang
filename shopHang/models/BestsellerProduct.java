package com.shopHang.models;

public class BestsellerProduct {
    private String productId;
    private String name;
    private String category;
    private int totalSold;
    private double totalRevenue;
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public int getTotalSold() {
        return totalSold;
    }
    
    public void setTotalSold(int totalSold) {
        this.totalSold = totalSold;
    }
    
    public double getTotalRevenue() {
        return totalRevenue;
    }
    
    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
    }
    
    public String getFormattedRevenue() {
        return String.format("%,.0f₫", totalRevenue);
    }
}

