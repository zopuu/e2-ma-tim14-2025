package com.example.habibitar.data.user;
import com.google.firebase.firestore.DocumentSnapshot;
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
}
