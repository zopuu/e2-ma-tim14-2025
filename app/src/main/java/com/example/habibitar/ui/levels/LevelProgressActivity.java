package com.example.habibitar.ui.levels;

import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.habibitar.R;
import com.example.habibitar.domain.logic.LevelEngine;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class LevelProgressActivity extends AppCompatActivity {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_progress);

        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvLevel = findViewById(R.id.tvLevel);
        TextView tvPp    = findViewById(R.id.tvPp);
        TextView tvXp    = findViewById(R.id.tvXp);

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) { finish(); return; }

        db.collection("users").document(uid).get()
                .addOnSuccessListener(snap -> {
                    int level = snap.getLong("level") != null ? snap.getLong("level").intValue() : 1;
                    int pp    = snap.getLong("pp")    != null ? snap.getLong("pp").intValue()    : 0;
                    int xp    = snap.getLong("xp")    != null ? snap.getLong("xp").intValue()    : 0;
                    String title = snap.getString("title");
                    if (title == null) title = LevelEngine.titleForLevel(level);

                    int needForNext = LevelEngine.requiredXpForNextLevel(level);

                    tvTitle.setText(title);
                    tvLevel.setText(String.format(Locale.getDefault(), "Level %d", level));
                    tvPp.setText(String.format(Locale.getDefault(), "PP: %d", pp));
                    tvXp.setText(String.format(Locale.getDefault(), "XP: %d / %d", xp, needForNext));
                })
                .addOnFailureListener(err -> finish());
    }
}
