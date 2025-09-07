package com.example.habibitar.util;

import androidx.annotation.Nullable;

public class QRParse {
    @Nullable
    public static String tryGetUidFromPayload(String payload) {
        if (payload == null) return null;
        payload = payload.trim();
        String prefix = "user:";
        if (!payload.startsWith(prefix)) return null;
        String uid = payload.substring(prefix.length());
        return uid.isEmpty() ? null : uid;
    }
}
