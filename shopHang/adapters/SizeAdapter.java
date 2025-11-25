package com.shopHang.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.shopHang.R;
import java.util.ArrayList;
import java.util.List;

public class SizeAdapter extends RecyclerView.Adapter<SizeAdapter.ViewHolder> {
    private List<String> sizes;
    private String selectedSize;
    private SizeClickListener listener;
    
    public interface SizeClickListener {
        void onSizeClick(String size);
    }
    
    public SizeAdapter(List<String> sizes, SizeClickListener listener) {
        this.sizes = new ArrayList<>(sizes);
        this.listener = listener;
    }
    
    public void updateSizes(List<String> newSizes) {
        this.sizes = new ArrayList<>(newSizes);
        notifyDataSetChanged();
    }
    
    public void setSelectedSize(String size) {
        String previousSelected = this.selectedSize;
        this.selectedSize = size;
        
        if (previousSelected != null) {
            int previousIndex = sizes.indexOf(previousSelected);
            if (previousIndex >= 0) {
                notifyItemChanged(previousIndex);
            }
        }
        if (size != null) {
            int currentIndex = sizes.indexOf(size);
            if (currentIndex >= 0) {
                notifyItemChanged(currentIndex);
            }
        }
    }
    
    public String getSelectedSize() {
        return selectedSize;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_size, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String size = sizes.get(position);
        boolean isSelected = size.equals(selectedSize);
        
        holder.tvSize.setText(size);
        holder.itemView.setSelected(isSelected);
        
        if (isSelected) {
            holder.itemView.setBackgroundResource(R.drawable.size_selected);
            holder.tvSize.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.white));
        } else {
            holder.itemView.setBackgroundResource(R.drawable.size_normal);
            holder.tvSize.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_primary));
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSizeClick(size);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return sizes.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSize;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSize = itemView.findViewById(R.id.tvSize);
        }
    }
}

