// CategoryAdapter.java
package com.example.habibitar.ui.profile;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.habibitar.R;
import com.example.habibitar.domain.model.Category;
import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.VH> {

    public interface OnItemClick {
        void onClick(Category item);
    }

    private final List<Category> items = new ArrayList<>();
    private final OnItemClick onItemClick;

    public CategoryAdapter(OnItemClick onItemClick) {
        this.onItemClick = onItemClick;
    }

    public void setItems(List<Category> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Category c = items.get(position);
        h.tvName.setText(c.getName());
        h.tvId.setText("id: " + c.getId());
        h.tvOwner.setText("ownerId: " + c.getOwnerId());
        h.tvColor.setText("colorCode: " + c.getColorCode());
        try {
            h.viewColor.setBackgroundColor(Color.parseColor(c.getColorCode()));
        } catch (Exception ignore) { h.viewColor.setBackgroundColor(0xFFC1C1C1); }

        h.itemView.setOnClickListener(v -> {
            if (onItemClick != null) onItemClick.onClick(c);
        });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvId, tvOwner, tvColor;
        View viewColor;
        VH(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCategoryName);
            tvId   = itemView.findViewById(R.id.tvCategoryId);
            tvOwner= itemView.findViewById(R.id.tvOwnerId);
            tvColor= itemView.findViewById(R.id.tvColorCode);
            viewColor = itemView.findViewById(R.id.viewItemColor);
        }
    }
}
