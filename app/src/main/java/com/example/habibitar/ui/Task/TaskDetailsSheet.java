package com.example.habibitar.ui.Task;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.habibitar.R;
import com.example.habibitar.data.task.TaskRepository;
import com.example.habibitar.domain.model.Task;
import com.example.habibitar.domain.model.enums.Difficulty;
import com.example.habibitar.domain.model.enums.Frequency;
import com.example.habibitar.domain.model.enums.Importance;
import com.example.habibitar.domain.model.enums.RepetitionUnits;
import com.example.habibitar.domain.model.enums.TaskStatus;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class TaskDetailsSheet extends BottomSheetDialogFragment {
    public static final String TAG = "TaskDetailsSheet";
    private final SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private String labelDifficulty(@Nullable Difficulty d) {
        if (d == null) return "-";
        switch (d) {
            case VERY_EASY_1XP: return "Very Easy - 1XP";
            case EASY_3XP: return "Easy - 3XP";
            case HARD_7XP: return "Hard - 7XP";
            case EXTREAMLY_HARD_20XP: return "Extremely Hard - 20XP";
        }
        return d.name();
    }
    private String labelImportance(@Nullable Importance i) {
        if (i == null) return "-";
        switch (i) {
            case NORMAL_1XP: return "Normal - 1XP";
            case IMPORTANT_3XP: return "Important - 3XP";
            case EXTREAMLY_IMPORTANT_10XP: return "Extremely Important - 10XP";
            case SPECIAL_100XP: return "Special - 100XP";
        }
        return i.name();
    }
    private String labelFrequency(@Nullable Frequency f) {
        if (f == null) return "-";
        return f == Frequency.REPEATING ? "Repeating" : "Not repeating";
    }
    private String labelUnit(@Nullable RepetitionUnits u) {
        if (u == null) return "-";
        switch (u) {
            case DAY: return "day";
            case WEEK: return "week";
        }
        return u.name();
    }

    public static void show(androidx.fragment.app.FragmentManager fm, Task t) {
        TaskDetailsSheet s = new TaskDetailsSheet();
        Bundle b = new Bundle();
        b.putString("name", t.getName());
        b.putString("desc", t.getDescription());
        b.putString("catName", t.getCategoryName() != null ? t.getCategoryName() : "Category");
        b.putString("catColor", t.getCategoryColorCode() != null ? t.getCategoryColorCode() : "#FF9800");
        b.putString("taskId", t.getId());
        if (t.getStatus() != null) b.putString("status", t.getStatus().name());
        if (t.getStatus() != null) b.putString("status", t.getStatus().name());
        if (t.getDifficulty() != null) b.putString("difficulty", t.getDifficulty().name());
        if (t.getImportance() != null) b.putString("importance", t.getImportance().name());
        if (t.getFrequency() != null)  b.putString("frequency",  t.getFrequency().name());
        if (t.getUnitOfRepetition() != null) b.putString("unit", t.getUnitOfRepetition().name());
        if (t.getRepeatingInterval() != null) b.putInt("interval", t.getRepeatingInterval());
        if (t.getRepetitionStartDate() != null) b.putLong("start", t.getRepetitionStartDate().getTime());
        if (t.getRepetitionEndDate() != null)   b.putLong("end",   t.getRepetitionEndDate().getTime());
        s.setArguments(b);
        s.show(fm, TAG);
    }

    // TaskDetailsSheet.java (dodaj kao private metodu u klasu)
    private int xpFromEnumName(@Nullable Enum<?> e) {
        if (e == null) return 0;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d+)")
                .matcher(e.name());
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private int taskXp(@NonNull com.example.habibitar.domain.model.Task t) {
        return xpFromEnumName(t.getDifficulty()) + xpFromEnumName(t.getImportance());
    }


    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.task_popup_details, container, false);

        View color = v.findViewById(R.id.viewCategoryColor);
        TextView tvTitle = v.findViewById(R.id.tvTitle);
        TextView tvCategory = v.findViewById(R.id.tvCategory);
        TextView tvDesc = v.findViewById(R.id.tvDescription);
        TextView tvDifficulty = v.findViewById(R.id.tvDifficulty);
        TextView tvImportance = v.findViewById(R.id.tvImportance);
        TextView tvFrequency = v.findViewById(R.id.tvFrequency);
        View repeatSection = v.findViewById(R.id.repeatSection);
        TextView tvRepeatEvery = v.findViewById(R.id.tvRepeatEvery);
        TextView tvDateRange = v.findViewById(R.id.tvDateRange);
        Spinner spStatus = v.findViewById(R.id.spStatus);
        View btnSaveStatus = v.findViewById(R.id.btnSaveStatus);
        View btnDelete = v.findViewById(R.id.btnDelete);
        View btnEdit = v.findViewById(R.id.btnEdit);


        Bundle b = getArguments() != null ? getArguments() : new Bundle();
        String statusStr = b.getString("status", "ACTIVE");
        boolean isFinished = "FINISHED".equals(statusStr);

        btnDelete.setVisibility(isFinished ? View.GONE : View.VISIBLE);

        btnEdit.setVisibility(isFinished ? View.GONE : View.VISIBLE);

        btnDelete.setOnClickListener(view -> {
            String taskId = b.getString("taskId");
            if (taskId == null || taskId.isEmpty()) return;

            new com.example.habibitar.data.task.TaskRepository()
                    .delete(taskId)
                    .thenAccept(ignored -> {
                        Toast.makeText(requireContext(), "Task deleted.", Toast.LENGTH_SHORT).show();
                        dismiss();
                    })
                    .exceptionally(ex -> {
                        Toast.makeText(requireContext(),
                                "Delete failed: " + ex.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return null;
                    });
        });

        btnEdit.setOnClickListener(view -> {
            String taskId = b.getString("taskId");
            if (taskId == null || taskId.isEmpty()) return;
            android.content.Intent i = new android.content.Intent(requireContext(), TaskEditActivity.class);
            i.putExtra("taskId", taskId);
            startActivity(i);
            dismiss();
        });

        final String[] labels = new String[]{"Active","Paused","Finished","Canceled"};
        final TaskStatus[] enumValues = new TaskStatus[]{
                TaskStatus.ACTIVE, TaskStatus.PAUSED, TaskStatus.FINISHED, TaskStatus.CANCELED
        };

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                labels
        );
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStatus.setAdapter(statusAdapter);

        int selected = 0;
        if (b.containsKey("status")) {
            try {
                TaskStatus cur = TaskStatus.valueOf(b.getString("status"));
                for (int i = 0; i < enumValues.length; i++) {
                    if (enumValues[i] == cur) { selected = i; break; }
                }
            } catch (Exception ignored) {}
        }
        spStatus.setSelection(selected);

        TaskRepository repo = new TaskRepository();
        btnSaveStatus.setOnClickListener(v1 -> {
            String taskId = b.getString("taskId", null);
            if (taskId == null || taskId.isEmpty()) {
                Toast.makeText(requireContext(), "Task ID missing.", Toast.LENGTH_SHORT).show();
                return;
            }
            int pos = spStatus.getSelectedItemPosition();
            TaskStatus newStatus = enumValues[Math.max(0, Math.min(pos, enumValues.length - 1))];

            // bio je FINISHED?
            boolean wasFinished = "FINISHED".equals(b.getString("status", ""));

            btnSaveStatus.setEnabled(false);

            repo.updateStatus(taskId, newStatus)
                    .thenCompose(x -> {
                        // dodela XP samo ako ranije nije bio FINISHED, a sada jeste
                        if (!wasFinished && newStatus == TaskStatus.FINISHED) {
                            // Proveri xpGranted na dokumentu da izbegneš dupli XP
                            com.google.firebase.firestore.FirebaseFirestore db =
                                    com.google.firebase.firestore.FirebaseFirestore.getInstance();

                            return repo.getById(taskId)
                                    .thenCompose(task -> {
                                        // ako task nije pronađen
                                        if (task == null) return java.util.concurrent.CompletableFuture.completedFuture(null);

                                        String ownerId = task.getOwnerId();
                                        // prvo proveri xpGranted na sirovom snapu
                                        java.util.concurrent.CompletableFuture<Void> chain = new java.util.concurrent.CompletableFuture<>();
                                        db.collection("tasks").document(taskId).get()
                                                .addOnSuccessListener(snap -> {
                                                    Boolean xpGranted = snap.getBoolean("xpGranted");
                                                    if (Boolean.TRUE.equals(xpGranted)) {
                                                        // već dodeljeno
                                                        chain.complete(null);
                                                        return;
                                                    }
                                                    int xp = taskXp(task); // difficulty + importance
                                                    // 1) uvećaj XP korisniku
                                                    int baseDifficulty = xpFromEnumName(task.getDifficulty());   // e.g., 1/3/7/20
                                                    int baseImportance = xpFromEnumName(task.getImportance());   // e.g., 1/3/10/100
                                                    new com.example.habibitar.data.user.UserRepository()
                                                            .incrementXpWithLeveling(ownerId, baseDifficulty, baseImportance)
                                                            // 2) obeleži xpGranted = true na tasku
                                                            .thenCompose(ignored ->
                                                                    repo.updateTask(taskId,
                                                                            java.util.Collections.singletonMap("xpGranted", true)
                                                                    )
                                                            )
                                                            .thenAccept(ignored -> chain.complete(null))
                                                            .exceptionally(ex -> { chain.completeExceptionally(ex); return null; });
                                                })
                                                .addOnFailureListener(chain::completeExceptionally);
                                        return chain;
                                    });
                        }
                        // u svim drugim slučajevima nema dodatne akcije
                        return java.util.concurrent.CompletableFuture.completedFuture(null);
                    })
                    .thenAccept(ignored -> requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Status updated.", Toast.LENGTH_SHORT).show();
                        btnSaveStatus.setEnabled(true);
                        dismiss();
                    }))
                    .exceptionally(ex -> {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Failed: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                            btnSaveStatus.setEnabled(true);
                        });
                        return null;
                    });
        });


        btnDelete.setOnClickListener(view -> {
            String taskId = b.getString("taskId", null);
            if (taskId == null || taskId.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "Task ID missing.", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete task?")
                    .setMessage("This action cannot be undone.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete", (d, which) -> {
                        btnDelete.setEnabled(false);
                        repo.delete(taskId)
                                .thenAccept(x -> requireActivity().runOnUiThread(() -> {
                                    android.widget.Toast.makeText(requireContext(), "Task deleted.", android.widget.Toast.LENGTH_SHORT).show();
                                    btnDelete.setEnabled(true);
                                    dismiss(); 
                                }))
                                .exceptionally(ex -> {
                                    requireActivity().runOnUiThread(() -> {
                                        android.widget.Toast.makeText(requireContext(), "Failed: " + ex.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                                        btnDelete.setEnabled(true);
                                    });
                                    return null;
                                });
                    })
                    .show();
        });

        btnEdit.setOnClickListener(x -> {
            String taskId = b.getString("taskId", null);
            if (taskId == null || taskId.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "Task ID missing.", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            Intent i = new Intent(requireContext(), TaskEditActivity.class);
            i.putExtra("taskId", taskId);
            startActivity(i);
        });


        String name = b.getString("name", "");
        String desc = b.getString("desc", "");
        String catName = b.getString("catName", "Category");
        String catColor = b.getString("catColor", "#FF9800");

        Difficulty diff = null;
        Importance imp = null;
        Frequency freq = null;
        RepetitionUnits unit = null;

        if (b.containsKey("difficulty")) diff = Difficulty.valueOf(b.getString("difficulty"));
        if (b.containsKey("importance")) imp = Importance.valueOf(b.getString("importance"));
        if (b.containsKey("frequency"))  freq = Frequency.valueOf(b.getString("frequency"));
        if (b.containsKey("unit"))       unit = RepetitionUnits.valueOf(b.getString("unit"));

        tvTitle.setText(name);
        tvCategory.setText(catName);
        tvDesc.setText(desc.isEmpty() ? "—" : desc);
        tvDifficulty.setText("Difficulty: " + labelDifficulty(diff));
        tvImportance.setText("Importance: " + labelImportance(imp));
        tvFrequency.setText("Frequency: " + labelFrequency(freq));

        try { color.setBackgroundColor(Color.parseColor(catColor)); } catch (Exception ignored) { }

        boolean isRepeating = (freq == Frequency.REPEATING);
        repeatSection.setVisibility(isRepeating ? View.VISIBLE : View.GONE);
        if (isRepeating) {
            int interval = b.getInt("interval", 0);
            String every = interval > 0 ? ("Repeats every " + interval + " " + labelUnit(unit) + (interval > 1 ? "s" : "")) : "Repeats";
            tvRepeatEvery.setText(every);

            String range = "";
            if (b.containsKey("start")) {
                range += "From " + df.format(new java.util.Date(b.getLong("start")));
            }
            if (b.containsKey("end")) {
                if (!range.isEmpty()) range += " ";
                range += "to " + df.format(new java.util.Date(b.getLong("end")));
            }
            tvDateRange.setText(range);
        }

        v.findViewById(R.id.btnClose).setOnClickListener(view -> dismiss());
        return v;
    }
}
