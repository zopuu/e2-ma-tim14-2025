package com.example.habibitar.ui.friends;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habibitar.R;
import com.example.habibitar.domain.model.Friend;
import com.example.habibitar.util.Avatars;

public class FriendsAdapter extends ListAdapter<Friend, FriendsAdapter.VH> {

    public interface Listener {
        void onProfileClick(@NonNull Friend f);
    }

    private final Listener listener;

    public FriendsAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Friend> DIFF = new DiffUtil.ItemCallback<Friend>() {
        @Override public boolean areItemsTheSame(@NonNull Friend a, @NonNull Friend b) {
            return a.uid.equals(b.uid);
        }
        @Override public boolean areContentsTheSame(@NonNull Friend a, @NonNull Friend b) {
            return a.username.equals(b.username) &&
                    ((a.avatarKey == null && b.avatarKey == null) ||
                            (a.avatarKey != null && a.avatarKey.equals(b.avatarKey)));
        }
    };

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Friend f = getItem(position);
        h.tvUsername.setText(f.username);
        // Use your Avatars util; fallback to placeholder if null
        if (f.avatarKey != null) {
            h.imgAvatar.setImageResource(Avatars.map(f.avatarKey));
        } else {
            h.imgAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
        }
        h.btnProfile.setOnClickListener(v -> listener.onProfileClick(f));
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvUsername;
        ImageButton btnProfile, btnChat;
        VH(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgAvatar);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            btnProfile = itemView.findViewById(R.id.btnProfile);
        }
    }
}
