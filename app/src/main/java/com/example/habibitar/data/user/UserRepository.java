package com.example.habibitar.data.user;
import com.example.habibitar.domain.logic.LevelEngine;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
public class UserRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public CompletableFuture<DocumentSnapshot> getProfile(String uid){
        CompletableFuture<DocumentSnapshot> f = new CompletableFuture<>();
        db.collection("users").document(uid).get()
                .addOnSuccessListener(f::complete)
                .addOnFailureListener(f::completeExceptionally);
        return f;
    }

    /*public CompletableFuture<Void> incrementXp(String userId, int delta) {
        CompletableFuture<Void> fut = new CompletableFuture<>();
        if (userId == null || userId.isEmpty() || delta <= 0) {
            fut.complete(null); // nema čega da se doda
            return fut;
        }
        db.collection("users").document(userId)
                .update("xp", FieldValue.increment(delta))
                .addOnSuccessListener(v -> fut.complete(null))
                .addOnFailureListener(fut::completeExceptionally);
        return fut;
    }*/
    private String currentUid() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return null;
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    public CompletableFuture<Void> incrementXpWithLeveling(String userId, int baseTaskXpDifficulty, int baseTaskXpImportance) {
        CompletableFuture<Void> f = new CompletableFuture<>();
        String uid = (userId != null && !userId.isEmpty()) ? userId : currentUid();
        if (uid == null) { f.completeExceptionally(new IllegalStateException("Not authenticated")); return f; }

        DocumentReference doc = db.collection("users").document(uid);

        db.runTransaction(tx -> {
                    Map<String, Object> data = tx.get(doc).getData();
                    if (data == null) data = new HashMap<>();

                    int level = ((Number) data.getOrDefault("level", 1)).intValue();
                    int xp    = ((Number) data.getOrDefault("xp", 0)).intValue();
                    int pp    = ((Number) data.getOrDefault("pp", 0)).intValue();

                    // 1) Scale task XP by current level (spec: +50% per cleared level for both difficulty & importance)
                    int scaled = LevelEngine.totalTaskXpAtLevel(baseTaskXpDifficulty, baseTaskXpImportance, level);

                    // 2) Add XP and apply level-ups
                    xp += scaled;
                    LevelEngine.Result res = LevelEngine.applyLevelUps(level, xp);

                    int newLevel = res.newLevel;
                    int newXp    = res.newXp;
                    int gainedPP = res.gainedPp;
                    int newPp    = pp + gainedPP;

                    Map<String, Object> upd = new HashMap<>();
                    upd.put("xp", newXp);
                    upd.put("level", newLevel);
                    upd.put("pp", newPp);
                    upd.put("title", LevelEngine.titleForLevel(newLevel));
                    upd.put("updatedAt", System.currentTimeMillis());

                    tx.update(doc, upd);
                    return null;
                })
                .addOnSuccessListener(v -> f.complete(null))
                .addOnFailureListener(f::completeExceptionally);

        return f;
    }

    // Keep your existing simple increment if other features use it, but prefer the new one for tasks.
    public CompletableFuture<Void> incrementXp(String userId, int rawXp) {
        CompletableFuture<Void> f = new CompletableFuture<>();
        String uid = (userId != null && !userId.isEmpty()) ? userId : currentUid();
        if (uid == null) { f.completeExceptionally(new IllegalStateException("Not authenticated")); return f; }
        DocumentReference doc = db.collection("users").document(uid);
        db.runTransaction(tx -> {
            Map<String, Object> data = tx.get(doc).getData();
            if (data == null) data = new HashMap<>();
            int xp = ((Number) data.getOrDefault("xp", 0)).intValue();
            xp += rawXp;
            Map<String, Object> upd = new HashMap<>();
            upd.put("xp", xp);
            upd.put("updatedAt", System.currentTimeMillis());
            tx.update(doc, upd);
            return null;
        }).addOnSuccessListener(v -> f.complete(null)).addOnFailureListener(f::completeExceptionally);
        return f;
    }
}
