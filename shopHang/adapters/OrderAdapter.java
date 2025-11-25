package com.shopHang.adapters;

import android.graphics.Color;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {
    private List<Order> orders;
    
    public OrderAdapter(List<Order> orders) {
        this.orders = new ArrayList<>(orders);
    }
    
    public void updateOrders(List<Order> newOrders) {
        this.orders = new ArrayList<>(newOrders);
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orders.get(position);
        
        // Order ID and Date
        String orderId = order.getId() != null && order.getId().length() >= 8 
            ? order.getId().substring(0, 8).toUpperCase() 
            : (order.getId() != null ? order.getId().toUpperCase() : "N/A");
        holder.tvOrderId.setText("#DH" + orderId);
        
        // Format date: DD/MM/YYYY
        String dateStr = "";
        if (order.getCreatedAt() != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date date = inputFormat.parse(order.getCreatedAt());
                if (date != null) {
                    dateStr = outputFormat.format(date);
                } else {
                    // Fallback: just take first 10 chars if format is different
                    if (order.getCreatedAt().length() >= 10) {
                        dateStr = order.getCreatedAt().substring(0, 10);
                    }
                }
            } catch (Exception e) {
                // Fallback
                if (order.getCreatedAt().length() >= 10) {
                    dateStr = order.getCreatedAt().substring(0, 10);
                }
            }
        }
        holder.tvDate.setText(dateStr);
        
        // Status with color
        String status = order.getStatus();
        String statusText = order.getStatusText();
        holder.tvStatus.setText(statusText);
        
        // Set status dot color
        int statusColor = R.color.success; // default green
        if (status != null) {
            switch (status) {
                case "pending":
                    statusColor = R.color.warning; // orange
                    break;
                case "shipped":
                    statusColor = R.color.info; // blue
                    break;
                case "paid":
                    statusColor = R.color.success; // green
                    break;
                case "cancelled":
                    statusColor = R.color.error; // red
                    break;
            }
        }
        holder.statusDot.setBackgroundColor(holder.itemView.getContext().getResources().getColor(statusColor));
        
        // Product info
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            Order.OrderItem firstItem = order.getItems().get(0);
            int totalItems = order.getItems().size();
            
            // Product image
            if (firstItem.getProduct() != null && firstItem.getProduct().getImageUrl() != null 
                && !firstItem.getProduct().getImageUrl().isEmpty()) {
                String fullImageUrl = ImageUtils.getFullImageUrl(firstItem.getProduct().getImageUrl(), holder.itemView.getContext());
                Picasso.get()
                    .load(fullImageUrl)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(holder.ivProductImage);
            } else {
                holder.ivProductImage.setImageResource(R.drawable.ic_launcher_foreground);
            }
            
            // Product description
            String productName = firstItem.getProduct() != null ? firstItem.getProduct().getName() : "Sản phẩm";
            if (totalItems > 1) {
                holder.tvProductDescription.setText(productName + " và " + (totalItems - 1) + " sản phẩm khác");
            } else {
                holder.tvProductDescription.setText(productName);
            }
            
            // Total quantity
            int totalQuantity = 0;
            for (Order.OrderItem item : order.getItems()) {
                totalQuantity += item.getQuantity();
            }
            holder.tvQuantity.setText("Số lượng: " + totalQuantity);
        } else {
            holder.tvProductDescription.setText("Không có sản phẩm");
            holder.tvQuantity.setText("Số lượng: 0");
        }
        
        // Total amount
        holder.tvTotal.setText("Tổng cộng: " + order.getFormattedTotal());
        
        // Click listener để mở chi tiết đơn hàng
        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(v.getContext(), com.shopHang.customer.OrderDetailActivity.class);
            intent.putExtra("order", order);
            v.getContext().startActivity(intent);
        });
    }
    
    @Override
    public int getItemCount() {
        return orders.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvDate, tvStatus, tvProductDescription, tvQuantity, tvTotal;
        ImageView ivProductImage;
        View statusDot;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvProductDescription = itemView.findViewById(R.id.tvProductDescription);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            statusDot = itemView.findViewById(R.id.statusDot);
        }
    }
}

