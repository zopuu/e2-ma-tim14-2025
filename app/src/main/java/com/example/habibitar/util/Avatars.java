package com.example.habibitar.util;

import com.example.habibitar.R;

public final class Avatars {
    public static int map(String key) {
        if (key == null) return R.drawable.ic_avatar_placeholder;
        return switch (key) {
            case "a01" -> R.drawable.elephant;
            case "a02" -> R.drawable.eagle;
            case "a03" -> R.drawable.chameleon;
            case "a04" -> R.drawable.giraffe;
            case "a05" -> R.drawable.squirrel;
            default -> R.drawable.ic_avatar_placeholder;
        };
    }
}

