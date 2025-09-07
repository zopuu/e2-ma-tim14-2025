package com.example.habibitar.domain.model;

import androidx.annotation.Nullable;

public class Friend {
    public final String uid;
    public final String username;
    @Nullable public final String avatarKey; // can be null; we’ll map to a placeholder

    public Friend(String uid, String username, @Nullable String avatarKey) {
        this.uid = uid;
        this.username = username;
        this.avatarKey = avatarKey;
    }
}
