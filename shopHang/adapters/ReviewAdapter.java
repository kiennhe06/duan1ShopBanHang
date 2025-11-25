package com.shopHang.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.shopHang.R;
import com.shopHang.models.Review;
import com.shopHang.models.Order;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
    private List<Review> reviews;
    
    public ReviewAdapter(List<Review> reviews) {
        this.reviews = reviews;
    }
    
    public void updateReviews(List<Review> reviews) {
        this.reviews = reviews;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);
        holder.bind(review);
    }
    
    @Override
    public int getItemCount() {
        return reviews != null ? reviews.size() : 0;
    }
    
    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        private TextView tvCustomerName, tvRating, tvComment, tvDate, tvAdminReply, tvAdminReplyDate;
        private TextView tvOrderId, tvOrderDate, tvOrderTotal, tvOrderStatus, tvOrderLabel;
        private View adminReplyContainer, orderInfoContainer;
        
        ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAdminReply = itemView.findViewById(R.id.tvAdminReply);
            tvAdminReplyDate = itemView.findViewById(R.id.tvAdminReplyDate);
            adminReplyContainer = itemView.findViewById(R.id.adminReplyContainer);
            
            // Order info views
            orderInfoContainer = itemView.findViewById(R.id.orderInfoContainer);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvOrderTotal = itemView.findViewById(R.id.tvOrderTotal);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            tvOrderLabel = itemView.findViewById(R.id.tvOrderLabel);
        }
        
        void bind(Review review) {
            if (review.getCustomer() != null) {
                tvCustomerName.setText(review.getCustomer().getName() != null 
                    ? review.getCustomer().getName() 
                    : "Khách hàng");
            } else {
                tvCustomerName.setText("Khách hàng");
            }
            
            tvRating.setText(review.getRatingStars());
            tvComment.setText(review.getComment() != null ? review.getComment() : "");
            
            if (review.getCreatedAt() != null) {
                try {
                    String date = review.getCreatedAt().substring(0, 10);
                    tvDate.setText(date);
                } catch (Exception e) {
                    tvDate.setText("");
                }
            } else {
                tvDate.setText("");
            }
            
            // Hiển thị thông tin đơn hàng nếu có
            if (orderInfoContainer != null) {
                Order order = review.getOrder();
                if (order != null && order.getId() != null && !order.getId().isEmpty()) {
                    orderInfoContainer.setVisibility(View.VISIBLE);
                    
                    // Format order ID
                    String orderId = order.getId();
                    if (orderId != null && orderId.length() > 8) {
                        orderId = "#DH" + orderId.substring(0, 8).toUpperCase();
                    } else {
                        orderId = "#DH" + (orderId != null ? orderId.toUpperCase() : "");
                    }
                    if (tvOrderId != null) {
                        tvOrderId.setText(orderId);
                    }
                    
                    // Format order date
                    String orderDate = order.getCreatedAt();
                    if (orderDate != null && orderDate.length() >= 10) {
                        try {
                            String formattedDate = orderDate.substring(0, 10);
                            if (tvOrderDate != null) {
                                tvOrderDate.setText(formattedDate);
                            }
                        } catch (Exception e) {
                            if (tvOrderDate != null) {
                                tvOrderDate.setText(orderDate);
                            }
                        }
                    } else {
                        if (tvOrderDate != null) {
                            tvOrderDate.setText("");
                        }
                    }
                    
                    // Format order total
                    double totalAmount = order.getTotalAmount();
                    if (tvOrderTotal != null) {
                        tvOrderTotal.setText("Tổng tiền: " + formatCurrency(totalAmount));
                    }
                    
                    // Format order status
                    String status = order.getStatus();
                    if (status != null && !status.isEmpty()) {
                        String statusText = getStatusText(status);
                        if (tvOrderStatus != null) {
                            tvOrderStatus.setText(statusText);
                        }
                    } else {
                        if (tvOrderStatus != null) {
                            tvOrderStatus.setText("");
                        }
                    }
                } else {
                    // Không có thông tin đơn hàng, ẩn container
                    orderInfoContainer.setVisibility(View.GONE);
                }
            }
            
            // Hiển thị phản hồi admin nếu có
            if (review.hasAdminReply() && adminReplyContainer != null && tvAdminReply != null) {
                adminReplyContainer.setVisibility(View.VISIBLE);
                tvAdminReply.setText(review.getAdminReply());
                if (review.getAdminRepliedAt() != null && tvAdminReplyDate != null) {
                    try {
                        String date = review.getAdminRepliedAt().substring(0, 10);
                        tvAdminReplyDate.setText("Phản hồi từ Admin - " + date);
                    } catch (Exception e) {
                        tvAdminReplyDate.setText("Phản hồi từ Admin");
                    }
                } else if (tvAdminReplyDate != null) {
                    tvAdminReplyDate.setText("Phản hồi từ Admin");
                }
            } else if (adminReplyContainer != null) {
                adminReplyContainer.setVisibility(View.GONE);
            }
        }
        
        private String formatCurrency(double amount) {
            return String.format("%,.0f₫", amount);
        }
        
        private String getStatusText(String status) {
            if (status == null) return "";
            switch (status.toLowerCase()) {
                case "pending": return "Chờ xử lý";
                case "processing": return "Đang xử lý";
                case "delivering": return "Đang giao hàng";
                case "paid": return "Đã thanh toán";
                case "shipped": return "Đã giao hàng";
                case "cancelled": return "Đã hủy";
                default: return status;
            }
        }
    }
}

