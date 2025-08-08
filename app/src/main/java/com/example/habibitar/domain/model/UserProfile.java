package com.example.habibitar.domain.model;

import java.util.HashMap;
import java.util.Map;

public class UserProfile {
    public String uid;
    public String email;
    public String username;
    public String avatarKey;
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
        m.put("verificationSentAt", verificationSentAt);
        m.put("createdAt", createdAt);
        m.put("updatedAt", System.currentTimeMillis());
        return m;
    }
}
