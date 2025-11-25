package com.shopHang.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.shopHang.models.CartItem;
import com.shopHang.models.Product;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CartManager {
    private static final String CART_PREF_NAME = "cart_pref";
    private static final String KEY_CART_ITEMS = "cart_items";
    
    private static CartManager instance;
    private SharedPreferences sharedPreferences;
    private Gson gson;
    private List<CartItem> cartItems;
    
    private CartManager(Context context) {
        sharedPreferences = context.getSharedPreferences(CART_PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        loadCart();
    }
    
    public static synchronized CartManager getInstance(Context context) {
        if (instance == null) {
            instance = new CartManager(context);
        }
        return instance;
    }
    
    private void loadCart() {
        String cartJson = sharedPreferences.getString(KEY_CART_ITEMS, null);
        if (cartJson != null) {
            Type type = new TypeToken<List<CartItem>>(){}.getType();
            cartItems = gson.fromJson(cartJson, type);
        } else {
            cartItems = new ArrayList<>();
        }
    }
    
    private void saveCart() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_CART_ITEMS, gson.toJson(cartItems));
        editor.apply();
    }
    
    public void addToCart(Product product, int quantity) {
        addToCart(product, quantity, null);
    }
    
    public void addToCart(Product product, int quantity, String size) {
        for (CartItem item : cartItems) {
            // Kiểm tra cùng product và cùng size (nếu có)
            if (item.getProduct().getId().equals(product.getId())) {
                String itemSize = item.getSize();
                if ((size == null && itemSize == null) || (size != null && size.equals(itemSize))) {
                    item.setQuantity(item.getQuantity() + quantity);
                    saveCart();
                    return;
                }
            }
        }
        cartItems.add(new CartItem(product, quantity, size));
        saveCart();
    }
    
    public void removeFromCart(String productId) {
        cartItems.removeIf(item -> item.getProduct().getId().equals(productId));
        saveCart();
    }
    
    public void updateQuantity(String productId, int quantity) {
        for (CartItem item : cartItems) {
            if (item.getProduct().getId().equals(productId)) {
                if (quantity <= 0) {
                    removeFromCart(productId);
                } else {
                    item.setQuantity(quantity);
                }
                saveCart();
                return;
            }
        }
    }
    
    public List<CartItem> getCartItems() {
        return new ArrayList<>(cartItems);
    }
    
    public double getTotal() {
        double total = 0;
        for (CartItem item : cartItems) {
            total += item.getSubtotal();
        }
        return total;
    }
    
    public int getItemCount() {
        return cartItems.size();
    }
    
    public int getTotalQuantity() {
        int total = 0;
        for (CartItem item : cartItems) {
            total += item.getQuantity();
        }
        return total;
    }
    
    public void clearCart() {
        cartItems.clear();
        saveCart();
    }
    
    public boolean isEmpty() {
        return cartItems.isEmpty();
    }
}

