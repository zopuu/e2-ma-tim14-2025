package com.example.habibitar.data.user;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

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

    public CompletableFuture<Void> incrementXp(String userId, int delta) {
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
    }
}
