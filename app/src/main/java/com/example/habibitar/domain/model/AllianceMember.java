package com.example.habibitar.domain.model;

import androidx.annotation.Nullable;

public class AllianceMember {
    public final String uid;
    public final String username;
    @Nullable public final String avatarKey;
    public final String role; // "leader" | "member"
    public final long joinedAtMillis;

    public AllianceMember(String uid, String username, @Nullable String avatarKey, String role, long joinedAtMillis) {
        this.uid = uid;
        this.username = username;
        this.avatarKey = avatarKey;
        this.role = role;
        this.joinedAtMillis = joinedAtMillis;
    }
}
