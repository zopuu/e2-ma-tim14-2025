package com.example.habibitar.ui.Task;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habibitar.R;
import com.example.habibitar.data.task.TaskRepository;
import com.example.habibitar.domain.model.Task;
import com.example.habibitar.domain.model.enums.Frequency;
import com.example.habibitar.ui.profile.TaskAdapter;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class AllTasksActivity extends AppCompatActivity {
    private TaskRepository taskRepository = new TaskRepository();
    private RecyclerView rvNotRepeating, rvRepeating;
    private TaskAdapter adapterNotRepeating, adapterRepeating;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.all_tasks);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );


        rvNotRepeating = findViewById(R.id.rvNotRepeating);
        rvRepeating    = findViewById(R.id.rvRepeating);

        rvNotRepeating.setLayoutManager(new LinearLayoutManager(this));
        rvRepeating.setLayoutManager(new LinearLayoutManager(this));

        adapterNotRepeating = new TaskAdapter(new ArrayList<>(), task ->
                TaskDetailsSheet.show(getSupportFragmentManager(), task)
        );
        adapterRepeating = new TaskAdapter(new ArrayList<>(), task ->
                TaskDetailsSheet.show(getSupportFragmentManager(), task)
        );


        rvNotRepeating.setAdapter(adapterNotRepeating);
        rvRepeating.setAdapter(adapterRepeating);


        taskRepository.getAllForCurrentUser()
                .thenAccept(tasks -> runOnUiThread(() -> {
                    List<Task> notRep = new ArrayList<>();
                    List<Task> rep    = new ArrayList<>();

                    for (Task t : tasks) {
                        if (t.getFrequency() == Frequency.REPEATING) {
                            rep.add(t);
                        } else {
                            notRep.add(t);
                        }
                    }

                    adapterNotRepeating.setItems(notRep);
                    adapterRepeating.setItems(rep);

                    if (notRep.isEmpty() && rep.isEmpty()) {
                        Toast.makeText(this, "No tasks yet.", Toast.LENGTH_SHORT).show();
                    }
                }));
    }

    @Override
    protected void onResume() {
        super.onResume();
        taskRepository.getAllForCurrentUser()
                .thenAccept(tasks -> runOnUiThread(() -> {
                    java.util.List<com.example.habibitar.domain.model.Task> notRep = new java.util.ArrayList<>();
                    java.util.List<com.example.habibitar.domain.model.Task> rep = new java.util.ArrayList<>();
                    for (com.example.habibitar.domain.model.Task t : tasks) {
                        if (t.getFrequency() == com.example.habibitar.domain.model.enums.Frequency.REPEATING) {
                            rep.add(t);
                        } else {
                            notRep.add(t);
                        }
                    }
                    adapterNotRepeating.setItems(notRep);
                    adapterRepeating.setItems(rep);
                }));
    }

}
