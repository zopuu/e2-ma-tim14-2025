package com.example.habibitar.domain.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserProfile {
    public String uid;
    public String email;
    public String username;
    public String avatarKey;

    // RPG fields
    public int level;
    public String title;
    public int pp;
    public int xp;
    public int coins;
    public List<String> badges;
    public List<String> equipment;
    public String currentEquipment;
    public String qrPayload;

    // Meta
    public long verificationSentAt;
    public long createdAt;
    public long updatedAt;

    public UserProfile() {}

    public UserProfile(String uid, String email, String username, String avatarKey, long verificationSentAt) {
        this.uid = uid;
        this.email = email;
        this.username = username;
        this.avatarKey = avatarKey;
        this.verificationSentAt = verificationSentAt;

        // Defaults
        this.level = 1;
        this.title = "Novice";
        this.pp = 0;
        this.xp = 0;
        this.coins = 0;

        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("uid", uid);
        m.put("email", email);
        m.put("username", username);
        m.put("avatarKey", avatarKey);

        m.put("level", level);
        m.put("title", title);
        m.put("pp", pp);
        m.put("xp", xp);
        m.put("coins", coins);
        m.put("badges", badges);
        m.put("equipment", equipment);
        m.put("currentEquipment", currentEquipment);
        m.put("qrPayload", qrPayload);

        m.put("verificationSentAt", verificationSentAt);
        m.put("createdAt", createdAt);
        m.put("updatedAt", System.currentTimeMillis());
        return m;
    }
}
