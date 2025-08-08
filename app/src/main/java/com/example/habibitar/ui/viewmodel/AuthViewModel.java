package com.example.habibitar.ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.habibitar.data.auth.AuthRepository;
import com.example.habibitar.data.user.UserRepository;
import com.example.habibitar.util.Result;
import com.google.firebase.firestore.DocumentSnapshot;

public class AuthViewModel extends ViewModel {
    private final AuthRepository authRepo = new AuthRepository();
    private final UserRepository userRepo = new UserRepository();

    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<Boolean> navigateToMain = new MutableLiveData<>(false);
    private final long DAY_MS = 24 * 60 * 60 * 1000;

    public LiveData<String> getMessage() { return message;}
    public LiveData<Boolean> getNavigateToMain() { return navigateToMain; }

    public void register(String email,String password, String confirm, String username,String avatarKey){
        if (email.isEmpty() || password.length() < 6 || !password.equals(confirm) || username.isEmpty() || avatarKey.isEmpty()) {
            message.postValue("Please fill all fields correctly.");
            return;
        }
        authRepo.register(email, password, username, avatarKey).thenAccept(result -> {
            if (!result.success) {
                message.postValue("Registration failed: " + result.error);
                return;
            }
            message.postValue("Verification email sent. Please verify within 24h.");
        });
    }
    public void login(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            message.postValue("Email and password required.");
            return;
        }

        authRepo.login(email, password).thenAccept(res -> {
            if (!res.success) {
                message.postValue("Login failed: " + res.error);
                return;
            }

            if (!authRepo.isEmailVerified()) {
                // check 24h window
                String uid = authRepo.currentUid();
                if (uid == null) { message.postValue("Unexpected auth state."); return; }

                userRepo.getProfile(uid).thenAccept(doc -> {
                    Long sentAt = doc.getLong("verificationSentAt");
                    long now = System.currentTimeMillis();
                    if (sentAt == null) { sentAt = now - DAY_MS - 1; } // fail safe

                    if (now - sentAt > DAY_MS) {
                        // Optional: force re-registration by signOut and telling user to register again
                        authRepo.logout();
                        message.postValue("Activation link expired (24h). Please register again.");
                    } else {
                        message.postValue("Please verify your email before logging in.");
                    }
                }).exceptionally(ex -> {
                    message.postValue("Could not check verification state: " + ex.getMessage());
                    return null;
                });
                return;
            }

            // Verified → go to main
            navigateToMain.postValue(true);
        });
    }
    public void resendVerification() {
        authRepo.sendEmailVerification().thenAccept(res -> {
            message.postValue(res.success ? "Verification email re-sent." : "Resend failed: " + res.error);
        });
    }

    public void logout() {
        authRepo.logout();
        message.postValue("Logged out.");
    }
}
