package com.example.habibitar.ui.profile;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.habibitar.R;
import com.example.habibitar.data.auth.AuthRepository;
import com.example.habibitar.data.user.UserRepository;
import com.example.habibitar.domain.model.UserProfile;
import com.example.habibitar.util.Avatars;
import com.example.habibitar.util.QR;
import com.google.android.material.button.MaterialButton;
import com.google.zxing.WriterException;

public class ProfileActivity extends AppCompatActivity implements ProfilePresenter.View {

    public static final String EXTRA_UID = "uid";

    private ImageView imgAvatar, imgQR;
    private TextView tvUsername, tvLevelTitle, tvXP, tvPP, tvCoins, tvBadgesCount, tvCurrentEquipment, tvError;
    private View groupPrivate;
    private ProgressBar progress;
    private MaterialButton btnChangePassword;
    private BadgesAdapter badgesAdapter;
    private EquipmentAdapter equipmentAdapter;

    private ProfilePresenter presenter;
    private AuthRepository auth = new AuthRepository();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Find views
        imgAvatar = findViewById(R.id.imgAvatar);
        imgQR = findViewById(R.id.imgQR);
        tvUsername = findViewById(R.id.tvUsername);
        tvLevelTitle = findViewById(R.id.tvLevelTitle);
        tvXP = findViewById(R.id.tvXP);
        tvPP = findViewById(R.id.tvPP);
        tvCoins = findViewById(R.id.tvCoins);
        tvBadgesCount = findViewById(R.id.tvBadgesCount);
        tvCurrentEquipment = findViewById(R.id.tvCurrentEquipment);
        tvError = findViewById(R.id.tvError);
        groupPrivate = findViewById(R.id.groupPrivate);
        progress = findViewById(R.id.progress);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        // Lists
        androidx.recyclerview.widget.RecyclerView rvBadges = findViewById(R.id.rvBadges);
        rvBadges.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        badgesAdapter = new BadgesAdapter();
        rvBadges.setAdapter(badgesAdapter);

        androidx.recyclerview.widget.RecyclerView rvEquipment = findViewById(R.id.rvEquipment);
        rvEquipment.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        equipmentAdapter = new EquipmentAdapter();
        rvEquipment.setAdapter(equipmentAdapter);

        presenter = new ProfilePresenter(new UserRepository());

        String visitorUid = getIntent().getStringExtra(EXTRA_UID);
        String myUid = auth.currentUid();
        boolean isOwner = (visitorUid == null) || (myUid != null && myUid.equals(visitorUid));
        String targetUid = isOwner ? myUid : visitorUid;

        // Visibility decisions:
        // - If owner: show private group + change password.
        // - If visitor: hide them.
        groupPrivate.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        btnChangePassword.setVisibility(isOwner ? View.VISIBLE : View.GONE);

        if (targetUid == null) {
            showError("Not signed in.");
            return;
        }

        btnChangePassword.setOnClickListener(v -> ChangePasswordDialog.show(this));
        presenter.load(targetUid, isOwner, this);
    }

    @Override public void showLoading(boolean show) {
        progress.setVisibility(show ? View.VISIBLE : View.GONE);
        tvError.setVisibility(View.GONE);
    }

    @Override public void showError(@Nullable String message) {
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(message == null ? "Unknown error" : message);
    }

    @Override
    public void render(UserProfile p, boolean isOwner) {
        // avatar
        imgAvatar.setImageResource(Avatars.map(p.avatarKey));
        // username, level/title
        tvUsername.setText(p.username != null ? p.username : "user");
        String title = (p.title == null || p.title.isEmpty()) ? "Novice" : p.title;
        tvLevelTitle.setText("Level " + p.level + " • " + title);

        // public
        tvXP.setText(String.valueOf(p.xp));
        tvBadgesCount.setText(String.valueOf(p.badges != null ? p.badges.size() : 0));
        badgesAdapter.submit(p.badges);
        tvCurrentEquipment.setText(p.currentEquipment == null ? "None" : p.currentEquipment);
        // QR
        try {
            String payload = p.qrPayload != null ? p.qrPayload : ("user:" + p.uid);
            Bitmap bmp = QR.make(payload, 600);
            imgQR.setImageBitmap(bmp);
        } catch (WriterException e) {
            showError("Failed to render QR: " + e.getMessage());
        }

        // private (only owner)
        if (isOwner) {
            tvPP.setText(String.valueOf(p.pp));
            tvCoins.setText(String.valueOf(p.coins));
            equipmentAdapter.submit(p.equipment);
        }
    }
}
