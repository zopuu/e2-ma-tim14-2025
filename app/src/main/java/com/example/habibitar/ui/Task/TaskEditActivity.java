package com.example.habibitar.ui.Task;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habibitar.R;
import com.example.habibitar.data.task.TaskRepository;
import com.example.habibitar.domain.model.Task;
import com.example.habibitar.domain.model.enums.Difficulty;
import com.example.habibitar.domain.model.enums.Importance;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

public class TaskEditActivity extends AppCompatActivity {

    private TaskRepository taskRepo = new TaskRepository();
    private String taskId;

    private TextInputEditText etName, etDesc;
    private Spinner spDifficulty, spImportance;

    private Task loadedTask;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.task_edit);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );


        etName = findViewById(R.id.etTaskNameEdit);
        etDesc = findViewById(R.id.etDescriptionEdit);
        spDifficulty = findViewById(R.id.spDifficultyEdit);
        spImportance = findViewById(R.id.spImportanceEdit);

        spDifficulty.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{
                        "Very Easy - 1XP",
                        "Easy - 3XP",
                        "Hard - 7XP",
                        "Extremely Hard - 20XP"
                }));

        spImportance.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{
                        "Normal - 1XP",
                        "Important - 3XP",
                        "Extremely Important - 10XP",
                        "Special - 100XP"
                }));

        taskId = getIntent().getStringExtra("taskId");
        if (taskId == null || taskId.isEmpty()) {
            Toast.makeText(this, "Missing taskId", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        taskRepo.getById(taskId)
                .thenAccept(task -> runOnUiThread(() -> {
                    loadedTask = task;
                    if (task == null) {
                        Toast.makeText(this, "Task not found", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    if (task.getStatus() != null && task.getStatus().name().equals("FINISHED")) {
                        Toast.makeText(this, "Finished tasks cannot be edited.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    etName.setText(task.getName());
                    etDesc.setText(task.getDescription());
                    spDifficulty.setSelection(difficultyToIndex(task.getDifficulty()));
                    spImportance.setSelection(importanceToIndex(task.getImportance()));
                }))
                .exceptionally(ex -> {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                        finish();
                    });
                    return null;
                });

        Button btnSave = findViewById(R.id.btnSaveEdit);
        btnSave.setOnClickListener(v -> onSaveEdited());

        Button btnCancel = findViewById(R.id.btnCancelEdit);
        btnCancel.setOnClickListener(v -> finish());
    }

    private int difficultyToIndex(Difficulty d) {
        if (d == null) return 0;
        switch (d) {
            case VERY_EASY_1XP: return 0;
            case EASY_3XP: return 1;
            case HARD_7XP: return 2;
            case EXTREAMLY_HARD_20XP: return 3;
        }
        return 0;
    }

    private int importanceToIndex(Importance i) {
        if (i == null) return 0;
        switch (i) {
            case NORMAL_1XP: return 0;
            case IMPORTANT_3XP: return 1;
            case EXTREAMLY_IMPORTANT_10XP: return 2;
            case SPECIAL_100XP: return 3;
        }
        return 0;
    }

    private Difficulty indexToDifficulty(int idx) {
        switch (idx) {
            case 0: return Difficulty.VERY_EASY_1XP;
            case 1: return Difficulty.EASY_3XP;
            case 2: return Difficulty.HARD_7XP;
            default: return Difficulty.EXTREAMLY_HARD_20XP;
        }
    }

    private Importance indexToImportance(int idx) {
        switch (idx) {
            case 0: return Importance.NORMAL_1XP;
            case 1: return Importance.IMPORTANT_3XP;
            case 2: return Importance.EXTREAMLY_IMPORTANT_10XP;
            default: return Importance.SPECIAL_100XP;
        }
    }

    private void onSaveEdited() {
        if (loadedTask == null) return;

        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String desc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";

        if (name.isEmpty()) {
            etName.setError("Name is required");
            return;
        }

        Difficulty diff = indexToDifficulty(spDifficulty.getSelectedItemPosition());
        Importance imp  = indexToImportance(spImportance.getSelectedItemPosition());

        Map<String, Object> fields = new HashMap<>();
        fields.put("name", name);
        fields.put("description", desc);
        fields.put("difficulty", diff.name());
        fields.put("importance", imp.name());

        taskRepo.updateTask(taskId, fields)
                .thenAccept(v -> runOnUiThread(() -> {
                    Toast.makeText(this, "Task updated.", Toast.LENGTH_SHORT).show();
                    finish();
                }))
                .exceptionally(ex -> {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Failed: " + ex.getMessage(), Toast.LENGTH_LONG).show()
                    );
                    return null;
                });
    }
}
