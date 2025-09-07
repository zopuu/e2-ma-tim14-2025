package com.example.habibitar.ui.Task;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.habibitar.R;
import com.google.android.material.appbar.MaterialToolbar;

public class DisplayTasksActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.all_tasks);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }
}
