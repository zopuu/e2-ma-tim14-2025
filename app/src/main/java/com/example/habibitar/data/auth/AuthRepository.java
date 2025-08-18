package com.example.habibitar.data.auth;

import androidx.annotation.NonNull;

import com.example.habibitar.domain.model.UserProfile;
import com.example.habibitar.util.Result;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.rpc.context.AttributeContext;

import java.util.concurrent.CompletableFuture;
public class AuthRepository {
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public CompletableFuture<Result<Void>> register(String email, String password, String username, String avatarKey){
         CompletableFuture<Result<Void>> future = new CompletableFuture<>();
         auth.createUserWithEmailAndPassword(email,password)
                 .addOnCompleteListener(task -> {
                     if (!task.isSuccessful()) {
                         future.complete(Result.fail(task.getException() != null ? task.getException().getMessage() : "Registration failed!"));
                         return;
                     }
                     String uid = auth.getCurrentUser().getUid();
                     long sentAt = System.currentTimeMillis();

                     //Save profile
                     UserProfile profile = new UserProfile(uid,email,username,avatarKey,sentAt);
                     profile.qrPayload = "user:" + uid;
                     profile.badges = new java.util.ArrayList<>();
                     profile.equipment = new java.util.ArrayList<>();
                     profile.currentEquipment = null;

                     db.collection("users").document(uid).set(profile.toMap())
                             .addOnSuccessListener(unused -> {
                                 sendEmailVerification()
                                         .thenAccept(future::complete)
                                         .exceptionally(ex -> { future.complete(Result.fail(ex.getMessage())); return null;});
                             })
                             .addOnFailureListener(e -> future.complete(Result.fail(e.getMessage())));
                 });
                 return future;
    }
    public CompletableFuture<Result<Void>> sendEmailVerification() {
        CompletableFuture<Result<Void>> future = new CompletableFuture<>();
        if(auth.getCurrentUser() == null){
            future.complete(Result.fail("No user to verify!"));
            return future;
        }

        ActionCodeSettings settings = ActionCodeSettings.newBuilder()
                .setUrl("https://habibitar.firebaseapp.com/__/auth/action")
                .setAndroidPackageName("com.example.habibitar", true,null)
                .setHandleCodeInApp(true)
                .build();
        auth.getCurrentUser().sendEmailVerification(settings)
                .addOnSuccessListener(unused -> future.complete(Result.ok(null)))
                .addOnFailureListener(e -> future.complete(Result.fail(e.getMessage())));
        return future;
    }

    public CompletableFuture<Result<AuthResult>> login(String email, String password) {
        CompletableFuture<Result<AuthResult>> future = new CompletableFuture<>();

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        future.complete(Result.fail(
                                task.getException() != null
                                        ? task.getException().getMessage()
                                        : "Login failed"
                        ));
                        return;
                    }

                    // Dobij korisnika
                    if (auth.getCurrentUser() != null) {
                        if (auth.getCurrentUser().isEmailVerified()) {
                            // Email verifikovan → login uspešan
                            future.complete(Result.ok(task.getResult()));
                        } else {
                            // Email nije verifikovan → izloguj korisnika
                            auth.signOut();
                            future.complete(Result.fail("Please verify your email before logging in."));
                        }
                    } else {
                        future.complete(Result.fail("Login failed - no user found."));
                    }
                });

        return future;
    }

    public void logout() {auth.signOut();}
    public boolean isEmailVerified() {
        return auth.getCurrentUser() != null && auth.getCurrentUser().isEmailVerified();
    }
    public String currentUid() { return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null; }


}
