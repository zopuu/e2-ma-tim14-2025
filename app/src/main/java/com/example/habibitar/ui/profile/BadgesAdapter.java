package com.example.habibitar.ui.profile;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habibitar.R;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class BadgesAdapter extends RecyclerView.Adapter<BadgesAdapter.VH> {
    private final List<String> data = new ArrayList<>();

    static class VH extends RecyclerView.ViewHolder {
        Chip chip;
        VH(Chip c) { super(c); chip = c; }
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Chip chip = (Chip) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_badge, parent, false);
        return new VH(chip);
    }

    @Override public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.chip.setText(data.get(position));
    }

    @Override public int getItemCount() { return data.size(); }

    public void submit(List<String> badges) {
        data.clear();
        if (badges != null) data.addAll(badges);
        notifyDataSetChanged();
    }
}
