package com.example.habibitar.ui.friends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habibitar.R;
import com.example.habibitar.domain.model.Friend;
import com.example.habibitar.util.Avatars;

import java.util.ArrayList;
import java.util.List;

public class AddFriendResultsAdapter extends RecyclerView.Adapter<AddFriendResultsAdapter.VH> {

    public interface OnAddClick {
        void onAdd(@NonNull Friend target);
    }

    private final OnAddClick onAddClick;
    private final List<Friend> items = new ArrayList<>();

    public AddFriendResultsAdapter(OnAddClick onAddClick) {
        this.onAddClick = onAddClick;
    }

    public void submit(List<Friend> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_add_friend_result, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Friend f = items.get(position);
        h.tvUsername.setText(f.username);
        if (f.avatarKey != null) h.imgAvatar.setImageResource(Avatars.map(f.avatarKey));
        else h.imgAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
        h.btnAdd.setOnClickListener(v -> onAddClick.onAdd(f));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvUsername;
        ImageButton btnAdd;
        VH(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            btnAdd = itemView.findViewById(R.id.btnAdd);
        }
    }
}
