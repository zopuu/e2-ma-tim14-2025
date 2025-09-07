package com.example.habibitar.ui.profile;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.habibitar.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;

public final class ChangePasswordDialog {

    public static void show(Context ctx) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_change_password, null, false);
        EditText etOld = v.findViewById(R.id.etOldPassword);
        EditText etNew = v.findViewById(R.id.etNewPassword);
        EditText etConfirm = v.findViewById(R.id.etConfirmPassword);

        new AlertDialog.Builder(ctx)
                .setTitle("Change password")
                .setView(v)
                .setPositiveButton("Save", (d, which) -> {
                    String oldP = etOld.getText().toString().trim();
                    String newP = etNew.getText().toString().trim();
                    String conf = etConfirm.getText().toString().trim();

                    if (oldP.isEmpty() || newP.isEmpty() || conf.isEmpty()) {
                        Toast.makeText(ctx, "All fields are required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!newP.equals(conf)) {
                        Toast.makeText(ctx, "New passwords do not match", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newP.length() < 6) {
                        Toast.makeText(ctx, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (FirebaseAuth.getInstance().getCurrentUser() == null ||
                            FirebaseAuth.getInstance().getCurrentUser().getEmail() == null) {
                        Toast.makeText(ctx, "Not authenticated", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                    AuthCredential cred = EmailAuthProvider.getCredential(email, oldP);

                    FirebaseAuth.getInstance().getCurrentUser()
                            .reauthenticate(cred)
                            .addOnSuccessListener(unused -> FirebaseAuth.getInstance().getCurrentUser()
                                    .updatePassword(newP)
                                    .addOnSuccessListener(u -> Toast.makeText(ctx, "Password updated", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(ctx, e.getMessage(), Toast.LENGTH_LONG).show()))
                            .addOnFailureListener(e -> Toast.makeText(ctx, "Old password incorrect", Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
