package com.example.habibitar.ui.friends;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.habibitar.R;
import com.example.habibitar.data.friends.FriendsRepository;
import com.example.habibitar.domain.model.Friend;
import com.example.habibitar.ui.profile.ProfileActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class FriendsActivity extends AppCompatActivity {

    private FriendsRepository repo = new FriendsRepository();
    private FriendsAdapter adapter;
    private View tvEmpty;
    private TextInputEditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        etSearch  = findViewById(R.id.etSearch);
        MaterialButton btnAdd    = findViewById(R.id.btnAddFriend);
        MaterialButton btnScanQR = findViewById(R.id.btnScanQR);
        tvEmpty = findViewById(R.id.tvEmpty);

        androidx.recyclerview.widget.RecyclerView rv = findViewById(R.id.rvFriends);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FriendsAdapter(new FriendsAdapter.Listener() {
            @Override public void onProfileClick(@NonNull Friend f) {
                Intent i = new Intent(FriendsActivity.this, com.example.habibitar.ui.profile.ProfileActivity.class);
                i.putExtra(ProfileActivity.EXTRA_UID, f.uid); // existing ProfileActivity supports EXTRA_UID
                startActivity(i);
            }
        });
        rv.setAdapter(adapter);

        // Load my friends (stubbed now)
        findViewById(R.id.toolbar).post(this::loadFriends);

        // Filter list locally by username as user types (client-side filter over loaded list)
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFriends(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Open modal add friend (search remote users + add)
        btnAdd.setOnClickListener(v -> openAddFriendSheet());

        // Start QR flow (stub)
        btnScanQR.setOnClickListener(v -> {
            // TODO: integrate ZXing. For now, show a toast-like lightweight sheet.
            BottomSheetDialog b = new BottomSheetDialog(this);
            View content = LayoutInflater.from(this).inflate(R.layout.sheet_qr_stub, null, false);
            b.setContentView(content);
            b.show();
        });
    }

    // keep original list so filter is client-side
    private final List<Friend> fullList = new ArrayList<>();

    private void loadFriends() {
        setLoading(true);
        repo.loadMyFriends().thenAccept(list -> runOnUiThread(() -> {
            setLoading(false);
            fullList.clear();
            fullList.addAll(list);
            adapter.submitList(new ArrayList<>(fullList));
            tvEmpty.setVisibility(fullList.isEmpty() ? View.VISIBLE : View.GONE);
        }));
    }

    private void filterFriends(@NonNull String query) {
        if (query.isEmpty()) {
            adapter.submitList(new ArrayList<>(fullList));
            tvEmpty.setVisibility(fullList.isEmpty() ? View.VISIBLE : View.GONE);
            return;
        }
        String q = query.toLowerCase();
        ArrayList<Friend> filtered = new ArrayList<>();
        for (Friend f : fullList) {
            if (f.username.toLowerCase().contains(q)) filtered.add(f);
        }
        adapter.submitList(filtered);
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openAddFriendSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View content = LayoutInflater.from(this).inflate(R.layout.sheet_add_friend, null, false);
        sheet.setContentView(content);

        TextInputEditText etQuery = content.findViewById(R.id.etQuery);
        MaterialButton btnSearch = content.findViewById(R.id.btnSearch);
        androidx.recyclerview.widget.RecyclerView rvResults = content.findViewById(R.id.rvResults);
        ProgressBar progress = content.findViewById(R.id.progress);
        TextView tvNoResults = content.findViewById(R.id.tvNoResults);
        MaterialButton btnScanQR = content.findViewById(R.id.btnScanQR);

        rvResults.setLayoutManager(new LinearLayoutManager(this));
        AtomicReference<AddFriendResultsAdapter> resAdapter = new AtomicReference<>(
                new AddFriendResultsAdapter(target -> {
                    // click "Add" on a result
                    progress.setVisibility(View.VISIBLE);
                    repo.addFriend(target.uid).thenAccept(ok ->
                            runOnUiThread(() -> {
                                progress.setVisibility(View.GONE);
                                if (ok) {
                                    // refresh main list
                                    loadFriends();
                                    sheet.dismiss();
                                } else {
                                    tvNoResults.setText("Failed to add friend. Try again.");
                                    tvNoResults.setVisibility(View.VISIBLE);
                                }
                            })
                    );
                })
        );
        rvResults.setAdapter(resAdapter.get());

        btnSearch.setOnClickListener(v -> {
            String q = etQuery.getText() == null ? "" : etQuery.getText().toString().trim();
            if (q.isEmpty()) {
                tvNoResults.setText("Type a username.");
                tvNoResults.setVisibility(View.VISIBLE);
                return;
            }
            tvNoResults.setVisibility(View.GONE);
            progress.setVisibility(View.VISIBLE);
            repo.searchUsers(q).thenAccept(list ->
                    runOnUiThread(() -> {
                        progress.setVisibility(View.GONE);
                        resAdapter.get().submit(list);
                        tvNoResults.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                    })
            );
        });

        btnScanQR.setOnClickListener(v -> {
            // TODO: integrate ZXing scanner here and parse user UID from QR
            tvNoResults.setText("QR scanner coming soon.");
            tvNoResults.setVisibility(View.VISIBLE);
        });

        sheet.show();
    }

    private void setLoading(boolean loading) {
        // optional: you can add a small progress bar at toolbar level
        // or disable inputs while loading. For simplicity, no global spinner here.
    }
}
