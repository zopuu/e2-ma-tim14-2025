package com.example.habibitar.domain.model;

import androidx.annotation.Nullable;

public class Alliance {
    public final String id;
    public final String name;
    public final String ownerUid;
    public final int membersCount; // convenient for UI
    public final String ownerUsername;
    public final boolean missionActive;

    public Alliance(String id, String name, String ownerUid, int membersCount, String ownerUsername, boolean missionActive) {
        this.id = id;
        this.name = name;
        this.ownerUid = ownerUid;
        this.membersCount = membersCount;
        this.ownerUsername = ownerUsername;
        this.missionActive = missionActive;
    }
}
