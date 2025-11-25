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

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {
    private List<String> categories;
    private String selectedCategory;
    private CategoryClickListener listener;
    
    public interface CategoryClickListener {
        void onCategoryClick(String category);
    }
    
    public CategoryAdapter(List<String> categories, CategoryClickListener listener) {
        this.categories = new ArrayList<>(categories);
        this.listener = listener;
        this.selectedCategory = null; // "Tất cả" được chọn mặc định
    }
    
    public void updateCategories(List<String> newCategories) {
        this.categories = new ArrayList<>(newCategories);
        notifyDataSetChanged();
    }
    
    public void setSelectedCategory(String category) {
        String previousSelected = this.selectedCategory;
        this.selectedCategory = category;
        
        // Notify previous and current selection changed
        if (previousSelected != null) {
            int previousIndex = categories.indexOf(previousSelected);
            if (previousIndex >= 0) {
                notifyItemChanged(previousIndex);
            }
        }
        if (category != null) {
            int currentIndex = categories.indexOf(category);
            if (currentIndex >= 0) {
                notifyItemChanged(currentIndex);
            }
        }
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_filter, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String category = categories.get(position);
        boolean isSelected = category.equals(selectedCategory);
        
        holder.tvCategory.setText(category);
        holder.itemView.setSelected(isSelected);
        
        // Update background based on selection
        if (isSelected) {
            holder.itemView.setBackgroundResource(R.drawable.category_filter_selected);
            holder.tvCategory.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.white));
        } else {
            holder.itemView.setBackgroundResource(R.drawable.category_filter_normal);
            holder.tvCategory.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_primary));
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClick(category);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return categories.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
        }
    }
}

