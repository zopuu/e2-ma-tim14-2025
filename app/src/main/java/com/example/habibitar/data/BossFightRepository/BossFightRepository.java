package com.example.habibitar.data.BossFightRepository;

import android.util.Log;

import com.example.habibitar.domain.model.BossFight;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class BossFightRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final String COLLECTION = "bossFights";

    public CompletableFuture<BossFight> getActiveForUser(String uid) {
        CompletableFuture<BossFight> fut = new CompletableFuture<>();
        db.collection(COLLECTION)
                .whereEqualTo("userId", uid)
                .whereEqualTo("finished", false)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING) // 👈 novo
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> {
                    if (qs.isEmpty()) { fut.complete(null); return; }
                    DocumentSnapshot d = qs.getDocuments().get(0);
                    BossFight f = d.toObject(BossFight.class);
                    if (f != null && (f.getId() == null || f.getId().isEmpty())) {
                        try {
                            java.lang.reflect.Field field = BossFight.class.getDeclaredField("id");
                            field.setAccessible(true);
                            field.set(f, d.getId());
                        } catch (Exception ignored) {}
                    }
                    fut.complete(f);
                })
                .addOnFailureListener(fut::completeExceptionally);
        return fut;
    }


    public CompletableFuture<BossFight> create(String uid, BossFight fight) {
        CompletableFuture<BossFight> fut = new CompletableFuture<>();
        DocumentReference doc = db.collection(COLLECTION).document();

        Map<String, Object> data = new HashMap<>();
        data.put("id", doc.getId());
        data.put("userId", uid);
        data.put("level", fight.getLevel());
        data.put("bossHp", fight.getBossXp());
        data.put("usersAttacksLeft", fight.getUsersAttacksLeft());
        data.put("moneyReward", fight.getMoneyReward());
        data.put("itemsWon", fight.getItemsWon());
        data.put("itemsActivated", fight.isItemsActivated());
        data.put("finished", false);
        data.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

        doc.set(data)
                .addOnSuccessListener(v -> {
                    fight.setId(doc.getId());
                    fut.complete(fight);
                })
                .addOnFailureListener(fut::completeExceptionally);

        return fut;
    }

    public CompletableFuture<Void> updateState(String fightId, int bossHp, int attacksLeft) {
        CompletableFuture<Void> fut = new CompletableFuture<>();
        if (fightId == null || fightId.isEmpty()) { fut.complete(null); return fut; }
        Map<String, Object> fields = new HashMap<>();
        fields.put("bossHp", bossHp);
        fields.put("usersAttacksLeft", attacksLeft);
        db.collection(COLLECTION).document(fightId)
                .update(fields)
                .addOnSuccessListener(v -> fut.complete(null))
                .addOnFailureListener(e -> {
                    Log.e("BossFightRepo", "updateState failed", e);
                    fut.completeExceptionally(e);
                });
        return fut;
    }

    public CompletableFuture<Void> finish(String fightId) {
        CompletableFuture<Void> fut = new CompletableFuture<>();
        if (fightId == null || fightId.isEmpty()) { fut.complete(null); return fut; }
        db.collection(COLLECTION).document(fightId)
                .update("finished", true)
                .addOnSuccessListener(v -> fut.complete(null))
                .addOnFailureListener(fut::completeExceptionally);
        return fut;
    }


}
