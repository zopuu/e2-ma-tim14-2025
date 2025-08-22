package com.example.habibitar.ui.alliance;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.habibitar.R;
import com.example.habibitar.data.alliance.AllianceRepository;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class AllianceMembersSheet extends BottomSheetDialogFragment {

    private static final String ARG_AID = "aid";

    public static AllianceMembersSheet newInstance(@NonNull String allianceId) {
        Bundle b = new Bundle();
        b.putString(ARG_AID, allianceId);
        AllianceMembersSheet s = new AllianceMembersSheet();
        s.setArguments(b);
        return s;
    }

    private final AllianceRepository repo = new AllianceRepository();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inf.inflate(R.layout.sheet_alliance_members, container, false);
    }

    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        String aid = getArguments() != null ? getArguments().getString(ARG_AID) : null;
        if (aid == null) { dismissAllowingStateLoss(); return; }

        TextView tvTitle = v.findViewById(R.id.tvTitle);
        ProgressBar progress = v.findViewById(R.id.progress);
        TextView tvEmpty = v.findViewById(R.id.tvEmpty);
        androidx.recyclerview.widget.RecyclerView rv = v.findViewById(R.id.rvMembers);

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        MembersAdapter adapter = new MembersAdapter();
        rv.setAdapter(adapter);

        progress.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        repo.loadMembers(aid).thenAccept(list -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                progress.setVisibility(View.GONE);
                if (list == null || list.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    adapter.submit(java.util.Collections.emptyList());
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    adapter.submit(list);
                }
            });
        }).exceptionally(ex -> {
            if (!isAdded()) return null;
            requireActivity().runOnUiThread(() -> {
                progress.setVisibility(View.GONE);
                tvEmpty.setText(ex.getMessage() == null ? "Failed to load members." : ex.getMessage());
                tvEmpty.setVisibility(View.VISIBLE);
            });
            return null;
        });
    }
}
