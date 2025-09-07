package com.example.habibitar.ui.profile;

import androidx.annotation.Nullable;

import com.example.habibitar.data.user.UserRepository;
import com.example.habibitar.domain.model.UserProfile;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProfilePresenter {

    private final UserRepository repo;

    public interface View {
        void showLoading(boolean show);
        void showError(@Nullable String message);
        void render(UserProfile profile, boolean isOwner);
    }

    public ProfilePresenter(UserRepository repo) {
        this.repo = repo;
    }

    public void load(String uid, boolean isOwner, View view) {
        view.showLoading(true);
        repo.getProfile(uid)
                .thenAccept(doc -> {
                    view.showLoading(false);
                    if (!doc.exists()) {
                        view.showError("Profile not found.");
                        return;
                    }
                    UserProfile p = toProfile(doc);
                    view.render(p, isOwner);
                })
                .exceptionally(ex -> {
                    view.showLoading(false);
                    view.showError(ex.getMessage());
                    return null;
                });
    }

    private UserProfile toProfile(DocumentSnapshot d) {
        UserProfile p = new UserProfile();
        p.uid = d.getString("uid");
        p.email = d.getString("email");
        p.username = d.getString("username");
        p.avatarKey = d.getString("avatarKey");
        Long level = d.getLong("level");
        p.level = level == null ? 1 : level.intValue();
        p.title = d.getString("title");
        Long pp = d.getLong("pp");
        p.pp = pp == null ? 0 : pp.intValue();
        Long xp = d.getLong("xp");
        p.xp = xp == null ? 0 : xp.intValue();
        Long coins = d.getLong("coins");
        p.coins = coins == null ? 0 : coins.intValue();

        List<String> badges = (List<String>) d.get("badges");
        p.badges = badges != null ? badges : new ArrayList<>();

        List<String> equipment = (List<String>) d.get("equipment");
        p.equipment = equipment != null ? equipment : new ArrayList<>();

        p.currentEquipment = d.getString("currentEquipment");
        p.qrPayload = d.getString("qrPayload");
        return p;
    }
}
