package com.example.habibitar.ui.alliance;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habibitar.R;
import com.example.habibitar.data.alliance.AllianceRepository;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class AllianceActivity extends AppCompatActivity {

    private AllianceRepository repo = new AllianceRepository();

    private View groupNoAlliance;
    private View groupAlliance;
    private TextView tvAllianceName, tvAllianceOwner, tvMembersCount;
    private @Nullable String currentAllianceId;
    private MaterialButton btnOpenChat; // placeholder
    private MaterialButton btnDisband, btnLeave;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alliance);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        tb.setNavigationOnClickListener(v -> onBackPressed());

        groupNoAlliance = findViewById(R.id.groupNoAlliance);
        groupAlliance   = findViewById(R.id.groupAlliance);
        tvAllianceName  = findViewById(R.id.tvAllianceName);
        tvAllianceOwner = findViewById(R.id.tvAllianceOwner);
        tvMembersCount  = findViewById(R.id.tvMembersCount);
        btnOpenChat     = findViewById(R.id.btnOpenChat);
        btnDisband = findViewById(R.id.btnDisband);
        btnLeave   = findViewById(R.id.btnLeave);

        findViewById(R.id.btnCreateAlliance).setOnClickListener(v -> openCreateAllianceSheet());
        btnOpenChat.setOnClickListener(v -> {
            // Placeholder for 7.2
            // TODO: start AllianceChatActivity with allianceId
        });
        btnDisband.setOnClickListener(v -> {
            if (currentAllianceId == null) return;
            btnDisband.setEnabled(false);
            repo.disbandAlliance(currentAllianceId).thenAccept(x ->
                    runOnUiThread(this::refresh)
            ).exceptionally(ex -> {
                runOnUiThread(() -> {
                    btnDisband.setEnabled(true);
                    Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
                });
                return null;
            });
        });

        btnLeave.setOnClickListener(v -> {
            if (currentAllianceId == null) return;
            btnLeave.setEnabled(false);
            repo.leaveAlliance(currentAllianceId).thenAccept(x ->
                    runOnUiThread(this::refresh)
            ).exceptionally(ex -> {
                runOnUiThread(() -> {
                    btnLeave.setEnabled(true);
                    Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
                });
                return null;
            });
        });

    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        final ProgressBar progress = findViewById(R.id.progress);
        progress.setVisibility(View.VISIBLE);
        repo.getMyAlliance().thenAccept(a ->
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    if (a == null) {
                        currentAllianceId = null;
                        groupNoAlliance.setVisibility(View.VISIBLE);
                        groupAlliance.setVisibility(View.GONE);
                    } else {
                        currentAllianceId = a.id;
                        groupNoAlliance.setVisibility(View.GONE);
                        groupAlliance.setVisibility(View.VISIBLE);

                        tvAllianceName.setText(a.name);
                        tvAllianceOwner.setText("Leader: " + a.ownerUsername); // ✅ username, not UID
                        tvMembersCount.setText("Members: " + a.membersCount);

                        String me = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

                        boolean isLeader = me.equals(a.ownerUid);
                        // Buttons visibility based on role:
                        btnDisband.setVisibility(isLeader ? View.VISIBLE : View.GONE);
                        btnLeave.setVisibility(isLeader ? View.GONE : View.VISIBLE);
                    }

                })
        ).exceptionally(ex -> {
            runOnUiThread(() -> {
                progress.setVisibility(View.GONE);
                groupNoAlliance.setVisibility(View.VISIBLE);
            });
            return null;
        });
    }

    private void openCreateAllianceSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.sheet_create_alliance, null, false);
        sheet.setContentView(view);

        TextInputEditText etName = view.findViewById(R.id.etAllianceName);
        MaterialButton btnCreate = view.findViewById(R.id.btnCreate);
        ProgressBar progress = view.findViewById(R.id.progress);

        btnCreate.setOnClickListener(v -> {
            String name = etName.getText() == null ? "" : etName.getText().toString().trim();
            if (TextUtils.isEmpty(name)) {
                etName.setError("Enter a name");
                return;
            }
            progress.setVisibility(View.VISIBLE);
            btnCreate.setEnabled(false);

            repo.createAllianceAndNotifyFriends(name).thenAccept(a ->
                    runOnUiThread(() -> {
                        progress.setVisibility(View.GONE);
                        sheet.dismiss();
                        refresh();
                        // You can also show a Snackbar: "Invites sent to your friends"
                    })
            ).exceptionally(ex -> {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    btnCreate.setEnabled(true);
                    etName.setError(ex.getMessage());
                });
                return null;
            });
        });

        sheet.show();
    }
}
