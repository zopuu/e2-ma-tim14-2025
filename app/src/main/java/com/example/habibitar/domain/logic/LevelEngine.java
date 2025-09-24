package com.example.habibitar.domain.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public final class LevelEngine {
    private LevelEngine() {}

    // --- XP thresholds ---
    // Required XP to go from level N -> N+1 (level is 1-based; level 1 -> 2 requires 200).
    public static int requiredXpForNextLevel(int currentLevel) {
        // f(1) = 200; f(n+1) = ceil(1.5 * f(n))
        int req = 200;
        for (int l = 1; l < currentLevel; l++) {
            req = (int) Math.ceil(req * 1.5);
        }
        return req;
    }

    // --- Task XP scaling ---
    // Scale a base XP value (e.g., 1,3,7,20 / 1,3,10,100) for the player's current level.
    // Each cleared level multiplies by 1.5 and rounds up.
    public static int scaleByLevel(int baseXp, int currentLevel) {
        int v = baseXp;
        for (int i = 1; i < currentLevel; i++) {
            v = (int) Math.ceil(v * 1.5);
        }
        return v;
    }

    public static int totalTaskXpAtLevel(int baseDifficultyXp, int baseImportanceXp, int currentLevel) {
        return scaleByLevel(baseDifficultyXp, currentLevel) + scaleByLevel(baseImportanceXp, currentLevel);
    }

    // --- PP gain per level ---
    // Returns how many PP you gain when you reach 'newLevel' (2 -> 40, 3 -> ceil(40*1.75), etc.)
    public static int ppGainForLevel(int newLevel) {
        if (newLevel <= 1) return 0;         // nothing at level 1 baseline
        if (newLevel == 2) return 40;        // first cleared level reward
        // from level 3 onward: previous gain * 1.75, rounded
        int gain = 40;
        for (int l = 3; l <= newLevel; l++) {
            gain = (int) Math.ceil(gain * 1.75);
        }
        return gain;
    }

    // --- Title mapping (customize as you like) ---
    public static String titleForLevel(int level) {
        switch (level) {
            case 1: return "Novice";
            case 2: return "Apprentice";
            case 3: return "Adept";
            default: return String.format(Locale.getDefault(), "Hero Lv.%d", level);
        }
    }

    // Utility: compute as many level-ups as XP allows, returning [newLevel, remainingXp, totalPpDelta]
    public static Result applyLevelUps(int currentLevel, int currentXp) {
        int level = currentLevel;
        int xp    = currentXp;
        int ppDelta = 0;

        while (true) {
            int needed = requiredXpForNextLevel(level);
            if (xp < needed) break;
            xp -= needed;
            level += 1;
            ppDelta += ppGainForLevel(level);
        }
        return new Result(level, xp, ppDelta);
    }

    public static final class Result {
        public final int newLevel;
        public final int newXp;
        public final int gainedPp;
        Result(int L, int X, int P) { newLevel = L; newXp = X; gainedPp = P; }
    }
}
