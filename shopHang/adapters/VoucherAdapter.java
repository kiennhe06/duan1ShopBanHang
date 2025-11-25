package com.shopHang.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.shopHang.R;
import com.shopHang.models.Voucher;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.ViewHolder> {
    private List<Voucher> vouchers;
    private VoucherClickListener listener;
    private String selectedVoucherCode;
    
    public interface VoucherClickListener {
        void onVoucherClick(Voucher voucher);
    }
    
    public VoucherAdapter(List<Voucher> vouchers, VoucherClickListener listener) {
        this.vouchers = new ArrayList<>(vouchers);
        this.listener = listener;
    }
    
    public void updateVouchers(List<Voucher> newVouchers) {
        this.vouchers = new ArrayList<>(newVouchers);
        notifyDataSetChanged();
    }
    
    public void setSelectedVoucher(String voucherCode) {
        this.selectedVoucherCode = voucherCode;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_voucher, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Voucher voucher = vouchers.get(position);
        if (voucher == null) {
            return;
        }
        
        holder.tvVoucherCode.setText(voucher.getCode() != null ? voucher.getCode() : "N/A");
        holder.tvVoucherName.setText(voucher.getName() != null ? voucher.getName() : "Mã giảm giá");
        
        // Hiển thị giá trị giảm giá
        String discountText;
        if ("percentage".equals(voucher.getDiscountType())) {
            discountText = String.format("%.0f%%", voucher.getDiscountValue());
            if (voucher.getMaxDiscountAmount() != null && voucher.getMaxDiscountAmount() > 0) {
                discountText += " (Tối đa: " + String.format("%,.0f₫", voucher.getMaxDiscountAmount()) + ")";
            }
        } else {
            discountText = String.format("%,.0f₫", voucher.getDiscountValue());
        }
        holder.tvDiscountValue.setText(discountText);
        
        // Hiển thị đơn tối thiểu
        if (voucher.getMinOrderAmount() > 0) {
            holder.tvMinOrder.setText("Đơn tối thiểu: " + String.format("%,.0f₫", voucher.getMinOrderAmount()));
            holder.tvMinOrder.setVisibility(View.VISIBLE);
        } else {
            holder.tvMinOrder.setVisibility(View.GONE);
        }
        
        // Hiển thị thời gian hiệu lực
        if (holder.tvVoucherTime != null) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                
                String timeText = "";
                if (voucher.getStartDate() != null && voucher.getEndDate() != null) {
                    try {
                        Date startDate = inputFormat.parse(voucher.getStartDate());
                        Date endDate = inputFormat.parse(voucher.getEndDate());
                        if (startDate != null && endDate != null) {
                            timeText = outputFormat.format(startDate) + " - " + outputFormat.format(endDate);
                        }
                    } catch (Exception e) {
                        // Fallback: hiển thị raw date nếu parse lỗi
                        if (voucher.getStartDate().length() >= 10 && voucher.getEndDate().length() >= 10) {
                            timeText = voucher.getStartDate().substring(0, 10) + " - " + voucher.getEndDate().substring(0, 10);
                        }
                    }
                }
                if (!timeText.isEmpty()) {
                    holder.tvVoucherTime.setText("⏰ " + timeText);
                    holder.tvVoucherTime.setVisibility(View.VISIBLE);
                } else {
                    holder.tvVoucherTime.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                if (holder.tvVoucherTime != null) {
                    holder.tvVoucherTime.setVisibility(View.GONE);
                }
            }
        }
        
        // Hiển thị số lượt còn lại
        if (holder.tvUsageInfo != null) {
            if (voucher.getUsageLimit() != null && voucher.getUsageLimit() > 0) {
                int remaining = voucher.getUsageLimit() - voucher.getUsedCount();
                holder.tvUsageInfo.setText("📊 Còn lại: " + remaining + "/" + voucher.getUsageLimit());
                holder.tvUsageInfo.setVisibility(View.VISIBLE);
            } else {
                holder.tvUsageInfo.setVisibility(View.GONE);
            }
        }
        
        // Hiển thị mô tả
        if (voucher.getDescription() != null && !voucher.getDescription().isEmpty()) {
            holder.tvVoucherDescription.setText(voucher.getDescription());
            holder.tvVoucherDescription.setVisibility(View.VISIBLE);
        } else {
            holder.tvVoucherDescription.setVisibility(View.GONE);
        }
        
        // Highlight nếu được chọn
        if (voucher.getCode() != null && voucher.getCode().equals(selectedVoucherCode)) {
            holder.itemView.setBackgroundResource(R.drawable.button_primary);
            holder.tvVoucherCode.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
            holder.tvVoucherName.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
            holder.tvDiscountValue.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
            if (holder.tvVoucherTime != null) {
                holder.tvVoucherTime.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
            }
            if (holder.tvUsageInfo != null) {
                holder.tvUsageInfo.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
            }
            if (holder.tvMinOrder != null) {
                holder.tvMinOrder.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
            }
            if (holder.tvVoucherDescription != null) {
                holder.tvVoucherDescription.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.white));
            }
        } else {
            holder.itemView.setBackgroundResource(R.drawable.edit_text_background);
            holder.tvVoucherCode.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.cart_primary));
            holder.tvVoucherName.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.cart_text_secondary));
            holder.tvDiscountValue.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.cart_primary));
            if (holder.tvVoucherTime != null) {
                holder.tvVoucherTime.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.cart_text_secondary));
            }
            if (holder.tvUsageInfo != null) {
                holder.tvUsageInfo.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.cart_text_secondary));
            }
            if (holder.tvMinOrder != null) {
                holder.tvMinOrder.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.cart_text_secondary));
            }
            if (holder.tvVoucherDescription != null) {
                holder.tvVoucherDescription.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.cart_text_secondary));
            }
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVoucherClick(voucher);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return vouchers != null ? vouchers.size() : 0;
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvVoucherCode, tvVoucherName, tvDiscountValue, tvMinOrder, tvVoucherDescription, tvVoucherTime, tvUsageInfo;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVoucherCode = itemView.findViewById(R.id.tvVoucherCode);
            tvVoucherName = itemView.findViewById(R.id.tvVoucherName);
            tvDiscountValue = itemView.findViewById(R.id.tvDiscountValue);
            tvMinOrder = itemView.findViewById(R.id.tvMinOrder);
            tvVoucherDescription = itemView.findViewById(R.id.tvVoucherDescription);
            tvVoucherTime = itemView.findViewById(R.id.tvVoucherTime);
            tvUsageInfo = itemView.findViewById(R.id.tvUsageInfo);
        }
    }
}

