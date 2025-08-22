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

    private final List<AllianceMember> data = new ArrayList<>();

    public void submit(List<AllianceMember> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_alliance_member, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int pos) {
        AllianceMember m = data.get(pos);
        h.tvUsername.setText(m.username);
        h.tvRole.setText("leader".equals(m.role) ? "Leader" : "Member");
        // Map your avatarKey → drawable (same helper you used on Profile)
        if (m.avatarKey != null) {
            h.imgAvatar.setImageResource(Avatars.map(m.avatarKey));
        } else {
            h.imgAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
        }
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvUsername, tvRole;
        VH(@NonNull View v) {
            super(v);
            imgAvatar = v.findViewById(R.id.imgAvatar);
            tvUsername = v.findViewById(R.id.tvUsername);
            tvRole = v.findViewById(R.id.tvRole);
        }
    }
}
