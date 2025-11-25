package com.shopHang.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.shopHang.R;
import com.shopHang.models.Product;
import com.shopHang.utils.ImageUtils;
import com.shopHang.utils.PicassoHelper;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {
    private List<Product> products;
    private ProductClickListener listener;
    
    public interface ProductClickListener {
        void onProductClick(Product product);
    }
    
    public ProductAdapter(List<Product> products, ProductClickListener listener) {
        this.products = new ArrayList<>(products);
        this.listener = listener;
    }
    
    public void updateProducts(List<Product> newProducts) {
        this.products = new ArrayList<>(newProducts);
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);
        if (product == null) {
            return; // Skip invalid products
        }
        
        holder.tvName.setText(product.getName() != null ? product.getName() : "N/A");
        holder.tvPrice.setText(product.getFormattedPrice());
        holder.tvCategory.setText(product.getCategoryName() != null && !product.getCategoryName().isEmpty() ? product.getCategoryName() : "N/A");
        
        // Load product image
        String imageUrl = product.getImageUrl();
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Chuyển đổi thành full URL nếu cần
            String fullImageUrl = ImageUtils.getFullImageUrl(imageUrl, holder.itemView.getContext());
            
            if (fullImageUrl != null) {
                try {
                    Picasso picasso = PicassoHelper.getPicasso(holder.itemView.getContext());
                    
                    // Cancel request cũ nếu có (tránh load ảnh sai khi scroll)
                    Picasso.get().cancelRequest(holder.ivImage);
                    
                    // Load ảnh với retry và error handling
                    picasso.load(fullImageUrl)
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .resize(400, 400)
                        .centerCrop()
                        .noFade()
                        .into(holder.ivImage, new com.squareup.picasso.Callback() {
                            @Override
                            public void onSuccess() {
                                holder.ivImage.setVisibility(android.view.View.VISIBLE);
                            }
                            
                            @Override
                            public void onError(Exception e) {
                                holder.ivImage.setVisibility(android.view.View.VISIBLE);
                            }
                        });
                } catch (Exception e) {
                    holder.ivImage.setImageResource(R.drawable.ic_launcher_foreground);
                }
            } else {
                holder.ivImage.setImageResource(R.drawable.ic_launcher_foreground);
            }
        } else {
            holder.ivImage.setImageResource(R.drawable.ic_launcher_foreground);
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return products.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvPrice, tvCategory;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivProductImage);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvCategory = itemView.findViewById(R.id.tvProductCategory);
        }
    }
}

