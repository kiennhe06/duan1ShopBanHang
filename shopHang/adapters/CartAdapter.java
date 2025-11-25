package com.shopHang.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.shopHang.R;
import com.shopHang.models.CartItem;
import com.shopHang.utils.CartManager;
import com.shopHang.utils.ImageUtils;
import com.shopHang.utils.PicassoHelper;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {
    private List<CartItem> cartItems;
    private CartListener listener;
    private java.util.Set<String> selectedProductIds = new java.util.HashSet<>();
    
    public interface CartListener {
        void onQuantityChanged();
        void onItemRemoved();
        void onSelectionChanged();
    }
    
    public CartAdapter(CartListener listener) {
        this.cartItems = new ArrayList<>();
        this.listener = listener;
    }
    
    public void updateCartItems(List<CartItem> items) {
        // Giữ lại selection của các sản phẩm vẫn còn trong giỏ
        java.util.Set<String> newSelectedIds = new java.util.HashSet<>();
        for (CartItem item : items) {
            if (item.getProduct() != null && selectedProductIds.contains(item.getProduct().getId())) {
                newSelectedIds.add(item.getProduct().getId());
            }
        }
        
        this.cartItems = new ArrayList<>(items);
        
        // Mặc định chọn tất cả nếu chưa có selection nào hoặc đây là lần đầu load
        if (selectedProductIds.isEmpty() || newSelectedIds.isEmpty()) {
            selectedProductIds.clear();
            for (CartItem item : items) {
                if (item.getProduct() != null) {
                    selectedProductIds.add(item.getProduct().getId());
                }
            }
        } else {
            // Giữ lại selection hiện tại
            selectedProductIds = newSelectedIds;
        }
        notifyDataSetChanged();
    }
    
    public List<CartItem> getSelectedItems() {
        List<CartItem> selected = new ArrayList<>();
        for (CartItem item : cartItems) {
            if (item.getProduct() != null && selectedProductIds.contains(item.getProduct().getId())) {
                selected.add(item);
            }
        }
        return selected;
    }
    
    public boolean hasSelectedItems() {
        return !selectedProductIds.isEmpty();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        if (item == null || item.getProduct() == null) {
            return; // Skip invalid items
        }
        
        holder.tvName.setText(item.getProduct().getName() != null ? item.getProduct().getName() : "N/A");
        holder.tvPrice.setText(String.format("%,.0f₫", item.getProduct().getPrice()));
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));
        
        // Hiển thị màu và size
        StringBuilder colorSizeText = new StringBuilder();
        // Giả sử có thể lấy màu từ product (nếu có field color)
        // Nếu không có, chỉ hiển thị size
        if (item.getSize() != null && !item.getSize().isEmpty()) {
            colorSizeText.append("Size: ").append(item.getSize());
        }
        if (colorSizeText.length() > 0) {
            holder.tvColorSize.setText(colorSizeText.toString());
            holder.tvColorSize.setVisibility(View.VISIBLE);
        } else {
            holder.tvColorSize.setVisibility(View.GONE);
        }
        
        // Load product image
        String imageUrl = item.getProduct().getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            String fullImageUrl = ImageUtils.getFullImageUrl(imageUrl, holder.itemView.getContext());
            if (fullImageUrl != null && !fullImageUrl.isEmpty()) {
                android.util.Log.d("CartAdapter", "Loading image: " + fullImageUrl);
                Picasso picasso = com.shopHang.utils.PicassoHelper.getPicasso(holder.itemView.getContext());
                picasso.load(fullImageUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .resize(400, 400)
                    .centerCrop()
                    .into(holder.ivImage, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            android.util.Log.d("CartAdapter", "Image loaded successfully: " + fullImageUrl);
                        }
                        
                        @Override
                        public void onError(Exception e) {
                            android.util.Log.e("CartAdapter", "Error loading image: " + fullImageUrl, e);
                        }
                    });
            } else {
                android.util.Log.w("CartAdapter", "Invalid image URL: " + imageUrl);
                holder.ivImage.setImageResource(R.drawable.ic_launcher_foreground);
            }
        } else {
            android.util.Log.d("CartAdapter", "No image URL for product: " + item.getProduct().getName());
            holder.ivImage.setImageResource(R.drawable.ic_launcher_foreground);
        }
        
        // Setup checkbox selection
        String productId = item.getProduct() != null ? item.getProduct().getId() : null;
        boolean isSelected = productId != null && selectedProductIds.contains(productId);
        
        // Set checkbox state - tắt listener tạm thời để tránh trigger khi setChecked
        holder.cbSelect.setOnCheckedChangeListener(null);
        holder.cbSelect.setChecked(isSelected);
        holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (productId != null) {
                if (isChecked) {
                    selectedProductIds.add(productId);
                } else {
                    selectedProductIds.remove(productId);
                }
                if (listener != null) {
                    listener.onSelectionChanged();
                }
            }
        });
        
        // Click vào item để xem chi tiết (trừ khi click vào checkbox hoặc các button)
        View.OnClickListener itemClickListener = v -> {
            android.content.Intent intent = new android.content.Intent(v.getContext(), com.shopHang.customer.ProductDetailActivity.class);
            intent.putExtra("product", item.getProduct());
            v.getContext().startActivity(intent);
        };
        
        holder.ivImage.setOnClickListener(itemClickListener);
        holder.tvName.setOnClickListener(itemClickListener);
        
        // Ngăn checkbox trigger item click
        holder.cbSelect.setOnClickListener(v -> {
            // Checkbox sẽ tự xử lý qua OnCheckedChangeListener
        });
        
        // Tăng/giảm số lượng - phải reload lại cart để cập nhật số lượng hiển thị
        holder.btnDecrease.setOnClickListener(v -> {
            int currentQuantity = item.getQuantity();
            if (currentQuantity > 1) {
                CartManager cartManager = CartManager.getInstance(v.getContext());
                cartManager.updateQuantity(item.getProduct().getId(), currentQuantity - 1);
                // Reload cart items để cập nhật số lượng
                List<CartItem> updatedItems = cartManager.getCartItems();
                updateCartItems(updatedItems);
                if (listener != null) listener.onQuantityChanged();
            }
        });
        
        holder.btnIncrease.setOnClickListener(v -> {
            int currentQuantity = item.getQuantity();
            if (currentQuantity < item.getProduct().getStock()) {
                CartManager cartManager = CartManager.getInstance(v.getContext());
                cartManager.updateQuantity(item.getProduct().getId(), currentQuantity + 1);
                // Reload cart items để cập nhật số lượng
                List<CartItem> updatedItems = cartManager.getCartItems();
                updateCartItems(updatedItems);
                if (listener != null) listener.onQuantityChanged();
            }
        });
        
        holder.btnRemove.setOnClickListener(v -> {
            String removedProductId = item.getProduct() != null ? item.getProduct().getId() : null;
            CartManager.getInstance(v.getContext()).removeFromCart(item.getProduct().getId());
            // Xóa khỏi selection nếu có
            if (removedProductId != null) {
                selectedProductIds.remove(removedProductId);
            }
            if (listener != null) listener.onItemRemoved();
        });
    }
    
    @Override
    public int getItemCount() {
        return cartItems.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox cbSelect;
        ImageView ivImage;
        TextView tvName, tvPrice, tvQuantity, tvColorSize;
        ImageButton btnDecrease, btnIncrease, btnRemove;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cbSelect = itemView.findViewById(R.id.cbSelect);
            ivImage = itemView.findViewById(R.id.ivProductImage);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvColorSize = itemView.findViewById(R.id.tvColorSize);
            btnDecrease = itemView.findViewById(R.id.btnDecrease);
            btnIncrease = itemView.findViewById(R.id.btnIncrease);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}

