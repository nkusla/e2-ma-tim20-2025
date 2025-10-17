package com.kulenina.questix.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.kulenina.questix.R;
import com.kulenina.questix.databinding.ItemCategoryBinding;
import com.kulenina.questix.model.Category;
import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<Category> categories;
    private final CategoryActionListener listener;

    public interface CategoryActionListener {
        void onDeleteClicked(Category category);
        void onColorChangeClicked(Category category);
    }

    public CategoryAdapter(CategoryActionListener listener) {
        this.categories = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCategoryBinding binding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.getContext()),
                R.layout.item_category,
                parent,
                false
        );
        return new CategoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);

        holder.binding.setCategory(category);
        holder.binding.setListener(listener);
        holder.binding.setCategoryColor(category.getColorHex());
        holder.binding.executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void setCategories(List<Category> newCategories) {
        this.categories.clear();
        if (newCategories != null) {
            this.categories.addAll(newCategories);
        }
        notifyDataSetChanged();
    }

    public static class CategoryViewHolder extends RecyclerView.ViewHolder {
        public ItemCategoryBinding binding;

        public CategoryViewHolder(@NonNull ItemCategoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}