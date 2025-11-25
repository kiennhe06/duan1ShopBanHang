package com.shopHang.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Product implements Serializable {
    @SerializedName("_id")
    private String id;
    private String name;
    private Object category; // Có thể là String hoặc Category object
    private double price;
    private int stock;
    private String description;
    private String imageUrl;
    private java.util.List<String> images;
    private java.util.List<String> sizes;
    
    public Product() {
    }
    
    public Product(String name, String category, double price, int stock, String description) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.description = description;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public Object getCategory() {
        return category;
    }
    
    public void setCategory(Object category) {
        this.category = category;
    }
    
    public String getCategoryName() {
        if (category == null) {
            return "";
        }
        if (category instanceof String) {
            return (String) category;
        }
        // Nếu là object, có thể parse từ JSON
        return category.toString();
    }
    
    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
    
    public int getStock() {
        return stock;
    }
    
    public void setStock(int stock) {
        this.stock = stock;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getFormattedPrice() {
        return String.format("%,.0f₫", price);
    }
    
    public java.util.List<String> getImages() {
        return images;
    }
    
    public void setImages(java.util.List<String> images) {
        this.images = images;
    }
    
    public java.util.List<String> getSizes() {
        return sizes;
    }
    
    public void setSizes(java.util.List<String> sizes) {
        this.sizes = sizes;
    }
}
