package com.example.habibitar.ui.auth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habibitar.R;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class AvatarsAdapter extends RecyclerView.Adapter<AvatarsAdapter.VH> {

    public static class AvatarOption {
        public final String key;
        public final @DrawableRes int resId;
        public AvatarOption(String key, int resId) { this.key = key; this.resId = resId; }
    }

    public interface OnSelect {
        void onSelected(AvatarOption option);
    }

    private final List<AvatarOption> data = new ArrayList<>();
    private int selected = RecyclerView.NO_POSITION;
    private final OnSelect callback;

    public AvatarsAdapter(OnSelect callback) {
        this.callback = callback;
    }

    public void submit(List<AvatarOption> items) {
        data.clear();
        data.addAll(items);
        selected = RecyclerView.NO_POSITION;
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avatar, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        AvatarOption opt = data.get(position);
        h.img.setImageResource(opt.resId);

        // selection UI: stroke when selected
        boolean isSelected = (position == selected);
        h.card.setStrokeWidth(isSelected ? 6 : 0);
        h.card.setStrokeColor(h.card.getResources().getColor(R.color.purple_200));

        h.itemView.setOnClickListener(v -> {
            int old = selected;
            selected = h.getAdapterPosition();
            notifyItemChanged(old);
            notifyItemChanged(selected);
            if (callback != null) callback.onSelected(opt);
        });
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        MaterialCardView card;
        VH(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.img);
            card = (MaterialCardView) itemView;
        }
    }
}
