package com.shopHang.models;

import java.io.Serializable;

public class CartItem implements Serializable {
    private Product product;
    private int quantity;
    private String size;
    
    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
        this.size = null;
    }
    
    public CartItem(Product product, int quantity, String size) {
        this.product = product;
        this.quantity = quantity;
        this.size = size;
    }
    
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
    
    public double getSubtotal() {
        return product.getPrice() * quantity;
    }
    
    public void increaseQuantity() {
        if (quantity < product.getStock()) {
            quantity++;
        }
    }
    
    public void decreaseQuantity() {
        if (quantity > 1) {
            quantity--;
        }
    }
    
    public String getSize() {
        return size;
    }
    
    public void setSize(String size) {
        this.size = size;
    }
}

