package com.shopHang.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.shopHang.R;
import com.shopHang.models.Order;
import com.shopHang.utils.ImageUtils;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.List;

public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.ViewHolder> {
    private List<Order.OrderItem> items;
    
    public OrderItemAdapter(List<Order.OrderItem> items) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
    }
    
    public void updateItems(List<Order.OrderItem> newItems) {
        this.items = newItems != null ? new ArrayList<>(newItems) : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_product, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order.OrderItem item = items.get(position);
        
        if (item.getProduct() != null) {
            // Product image
            if (item.getProduct().getImageUrl() != null && !item.getProduct().getImageUrl().isEmpty()) {
                String fullImageUrl = ImageUtils.getFullImageUrl(item.getProduct().getImageUrl(), holder.itemView.getContext());
                Picasso.get()
                    .load(fullImageUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(holder.ivProductImage);
            } else {
                holder.ivProductImage.setImageResource(R.drawable.ic_launcher_foreground);
            }
            
            holder.tvProductName.setText(item.getProduct().getName());
            holder.tvProductCategory.setText(item.getProduct().getCategoryName() != null && !item.getProduct().getCategoryName().isEmpty() ? item.getProduct().getCategoryName() : "N/A");
        } else {
            holder.ivProductImage.setImageResource(R.drawable.ic_launcher_foreground);
            holder.tvProductName.setText("Sản phẩm không xác định");
            holder.tvProductCategory.setText("N/A");
        }
        
        holder.tvQuantity.setText("Số lượng: " + item.getQuantity());
        holder.tvPrice.setText("Giá: " + String.format("%,.0f₫", item.getPrice()));
        holder.tvSubtotal.setText("Thành tiền: " + String.format("%,.0f₫", item.getSubtotal()));
    }
    
    @Override
    public int getItemCount() {
        return items.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProductImage;
        TextView tvProductName, tvProductCategory, tvQuantity, tvPrice, tvSubtotal;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductCategory = itemView.findViewById(R.id.tvProductCategory);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvSubtotal = itemView.findViewById(R.id.tvSubtotal);
        }
    }
}

