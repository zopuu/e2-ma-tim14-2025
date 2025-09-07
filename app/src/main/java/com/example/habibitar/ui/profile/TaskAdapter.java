// com.example.habibitar.ui.Task.TaskAdapter.java
package com.example.habibitar.ui.profile;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.view.View.OnClickListener;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habibitar.R;
import com.example.habibitar.domain.model.Task;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskVH> {

    public interface OnTaskClick {
        void onClick(Task task);
    }

    private final List<Task> items;
    private final OnTaskClick onTaskClick;
    private final SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public TaskAdapter(List<Task> items, OnTaskClick onTaskClick) {
        this.items = items;
        this.onTaskClick = onTaskClick;
    }

    @NonNull
    @Override
    public TaskVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskVH h, int position) {
        Task t = items.get(position);

        h.tvTaskName.setText(t.getName());
        h.tvCategoryName.setText(t.getCategoryName() != null ? t.getCategoryName() : "Category");

        String dateText = "";
        if (t.getRepetitionStart() != null) {
            dateText = "Start: " + df.format(t.getRepetitionStart());
        } else if (t.getRepetitionEnd() != null) {
            dateText = "End: " + df.format(t.getRepetitionEnd());
        }
        h.tvDateInfo.setText(dateText);

        int color = Color.parseColor("#FF9800");
        if (t.getCategoryColorCode() != null && !t.getCategoryColorCode().isEmpty()) {
            try {
                color = Color.parseColor(t.getCategoryColorCode());
            } catch (IllegalArgumentException ignored) { }
        }
        h.viewCategoryColor.setBackgroundColor(color);

        h.itemView.setOnClickListener(v -> {
            if (onTaskClick != null) onTaskClick.onClick(t);
        });
    }

    public void setItems(List<Task> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged(); 
    }


    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class TaskVH extends RecyclerView.ViewHolder {
        View viewCategoryColor;
        TextView tvTaskName;
        TextView tvCategoryName;
        TextView tvDateInfo;

        TaskVH(@NonNull View itemView) {
            super(itemView);
            viewCategoryColor = itemView.findViewById(R.id.viewCategoryColor);
            tvTaskName = itemView.findViewById(R.id.tvTaskName);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvDateInfo = itemView.findViewById(R.id.tvDateInfo);
        }
    }
}
