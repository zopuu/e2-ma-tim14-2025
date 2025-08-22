package com.example.habibitar.ui.alliance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habibitar.R;
import com.example.habibitar.domain.model.AllianceMember;
import com.example.habibitar.util.Avatars;

import java.util.ArrayList;
import java.util.List;

public class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.VH> {

    public interface OnMemberClick { void onMember(AllianceMember m); }

    private final List<AllianceMember> data = new ArrayList<>();
    private final OnMemberClick onClick;

    // NEW: pass a click handler
    public MembersAdapter(@NonNull OnMemberClick onClick) {
        this.onClick = onClick;
    }

    public void submit(List<AllianceMember> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alliance_member, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        AllianceMember m = data.get(pos);
        h.bind(m, onClick);
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvUsername, tvRole;

        VH(@NonNull View v) {
            super(v);
            imgAvatar = v.findViewById(R.id.imgAvatar);
            tvUsername = v.findViewById(R.id.tvUsername);
            tvRole = v.findViewById(R.id.tvRole); // make sure this id exists in item_alliance_member.xml
        }

        void bind(AllianceMember m, OnMemberClick onClick) {
            tvUsername.setText(m.username);
            tvRole.setText("leader".equals(m.role) ? "Leader" : "Member");
            imgAvatar.setImageResource(m.avatarKey != null ? Avatars.map(m.avatarKey)
                    : R.drawable.ic_avatar_placeholder);
            itemView.setOnClickListener(v -> { if (onClick != null) onClick.onMember(m); });
        }
    }
}
