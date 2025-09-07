package com.example.habibitar.ui.Task;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.habibitar.R;
import com.example.habibitar.data.task.TaskRepository;
import com.example.habibitar.domain.model.Task;
import com.example.habibitar.domain.model.enums.Difficulty;
import com.example.habibitar.domain.model.enums.Frequency;
import com.example.habibitar.domain.model.enums.Importance;
import com.example.habibitar.domain.model.enums.RepetitionUnits;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TaskCreationActivity extends AppCompatActivity {
    private TaskRepository taskRepository;
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private EditText etStartDateDatePicker;
    private EditText etEndDateDatePicker;
    private Spinner spinnerCategory;
    private com.example.habibitar.data.category.CategoryRepository categoryRepository;
    private java.util.List<com.example.habibitar.domain.model.Category> loadedCategories = new java.util.ArrayList<>();
    private android.widget.ArrayAdapter<String> categoryAdapter;

    private static final int[] REP_IDS = new int[] {
            R.id.tvRepeatingInterval,
            R.id.numberPickerRepeatingInterval,
            R.id.tvUnitOfRepetition,
            R.id.spinnerUnitOfRepetition,
            R.id.tvStartDate,
            R.id.etStartDate,
            R.id.tvEndDate,
            R.id.etEndDate
    };

    @Nullable
    private Date parseDateOrNull(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty()) return null;
        try {
            return dateFmt.parse(s);
        } catch (ParseException e) {
            return null;
        }
    }

    private void setRepetitionVisible(boolean show) {
        int vis = show ? View.VISIBLE : View.GONE;
        for (int vid : REP_IDS) {
            View v = findViewById(vid);
            if (v != null) v.setVisibility(vis);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.task_creation);

        etStartDateDatePicker = findViewById(R.id.etStartDate);
        etEndDateDatePicker   = findViewById(R.id.etEndDate);

        etStartDateDatePicker.setOnClickListener(v -> showDatePickerDialog(etStartDateDatePicker));
        etEndDateDatePicker.setOnClickListener(v -> showDatePickerDialog(etEndDateDatePicker));

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        NumberPicker numberPicker = findViewById(R.id.numberPickerRepeatingInterval);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(100000);
        numberPicker.setValue(1);


        Spinner spinnerDifficultyCategory = findViewById(R.id.spinnerDifficultyCategory);
        ArrayAdapter<CharSequence> adapterDifficulty = ArrayAdapter.createFromResource(
                this,
                R.array.difficulties,                          
                android.R.layout.simple_spinner_item         
        );
        adapterDifficulty.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        spinnerDifficultyCategory.setAdapter(adapterDifficulty);


        Spinner spinnerImportanceCategory = findViewById(R.id.spinnerImportanceCategory);
        ArrayAdapter<CharSequence> adapterImportance = ArrayAdapter.createFromResource(
                this,
                R.array.importance,
                android.R.layout.simple_spinner_item
        );
        adapterImportance.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerImportanceCategory.setAdapter(adapterImportance);


        String[] units = {"Day", "Week"};
        Spinner spinnerUnitOfRepetition = findViewById(R.id.spinnerUnitOfRepetition);
        ArrayAdapter<String> adapterUnitOfRepetition = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                units
        );
        adapterUnitOfRepetition.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUnitOfRepetition.setAdapter(adapterUnitOfRepetition);


        Spinner spinnerFrequencyCategory = findViewById(R.id.spinnerFrequencyCategory);

        ArrayAdapter<CharSequence> adapterFrequency = ArrayAdapter.createFromResource(
                this,
                R.array.frequencies,
                android.R.layout.simple_spinner_item
        );
        adapterFrequency.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFrequencyCategory.setAdapter(adapterFrequency);

        spinnerFrequencyCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = String.valueOf(parent.getItemAtPosition(position));
                boolean show = !"One-time".equalsIgnoreCase(selected); 
                setRepetitionVisible(show);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        Object sel = spinnerFrequencyCategory.getSelectedItem(); 
        boolean show = sel != null && !"One-time".equalsIgnoreCase(sel.toString());
        setRepetitionVisible(show);

        taskRepository = new TaskRepository();

        spinnerCategory = findViewById(R.id.spinnerCategory);
        categoryRepository = new com.example.habibitar.data.category.CategoryRepository();
        categoryAdapter = new android.widget.ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new java.util.ArrayList<>() 
        );
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        categoryRepository.getAllForCurrentUser()
                .thenAccept(categories -> runOnUiThread(() -> {
                    loadedCategories.clear();
                    loadedCategories.addAll(categories);

                    java.util.List<String> names = new java.util.ArrayList<>();
                    for (com.example.habibitar.domain.model.Category c : categories) {
                        names.add(c.getName());
                    }

                    categoryAdapter.clear();
                    categoryAdapter.addAll(names);
                    categoryAdapter.notifyDataSetChanged();
                }))
                .exceptionally(ex -> {
                    runOnUiThread(() ->
                            android.widget.Toast.makeText(this, "Failed to load categories: " + ex.getMessage(), android.widget.Toast.LENGTH_LONG).show()
                    );
                    return null;
                });
    }


    private void showDatePickerDialog(EditText target) {
        final Calendar cal = Calendar.getInstance();

        String existing = target.getText() != null ? target.getText().toString().trim() : "";
        if (!existing.isEmpty()) {
            try {
                Date parsed = dateFmt.parse(existing);
                if (parsed != null) {
                    cal.setTime(parsed);
                }
            } catch (ParseException ignored) { }
        }

        int y = cal.get(Calendar.YEAR);
        int m = cal.get(Calendar.MONTH);
        int d = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dlg = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(Calendar.YEAR, year);
                    picked.set(Calendar.MONTH, month);
                    picked.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    target.setText(dateFmt.format(picked.getTime()));
                },
                y, m, d
        );
        dlg.show();
    }


    private String getSelectedCategoryId() {
        int pos = spinnerCategory.getSelectedItemPosition();
        if (pos < 0 || pos >= loadedCategories.size()) return null;
        return loadedCategories.get(pos).getId();
    }

    public void onSaveClicked(View v) {
        Spinner spinnerFrequency = findViewById(R.id.spinnerFrequencyCategory);
        Spinner spinnerDifficulty = findViewById(R.id.spinnerDifficultyCategory);
        Spinner spinnerImportance = findViewById(R.id.spinnerImportanceCategory);
        EditText etName = findViewById(R.id.etTaskName);
        EditText etDescription = findViewById(R.id.etDescription);
        NumberPicker numberPicker = findViewById(R.id.numberPickerRepeatingInterval);
        Spinner spinnerUnitOfRepetition = findViewById(R.id.spinnerUnitOfRepetition);

        Integer repeatingInterval = numberPicker.getValue();
        String unitOfRepetition = spinnerUnitOfRepetition.getSelectedItem().toString();
        String categoryId = getSelectedCategoryId();
        String frequency = spinnerFrequency.getSelectedItem().toString();
        String difficulty = spinnerDifficulty.getSelectedItem().toString();
        String importance = spinnerImportance.getSelectedItem().toString();
        String name = etName.getText().toString();
        String description = etDescription.getText().toString();

        

        String testMsg = "Task: " + name +
                "\nDesc: " + description +
                "\nFreq: " + frequency +
                "\nDiff: " + difficulty +
                "\nImp: " + importance;



        Frequency frequencyEnumValue =
                "One-time".equalsIgnoreCase(frequency)
                        ? Frequency.NOT_REPEATING
                        : Frequency.REPEATING;

        Difficulty difficultyEnumValue;
        if ("Very Easy - 1XP".equalsIgnoreCase(difficulty)) {
            difficultyEnumValue = Difficulty.VERY_EASY_1XP;
        } else if ("Easy - 3XP".equalsIgnoreCase(difficulty)) {
            difficultyEnumValue = Difficulty.EASY_3XP;
        } else if ("Hard - 7XP".equalsIgnoreCase(difficulty)) {
            difficultyEnumValue = Difficulty.HARD_7XP;
        } else if ("Extreamly Hard - 20XP".equalsIgnoreCase(difficulty)
                || "Extremely Hard - 20XP".equalsIgnoreCase(difficulty)) {
            difficultyEnumValue = Difficulty.EXTREAMLY_HARD_20XP;
        } else {
            difficultyEnumValue = Difficulty.VERY_EASY_1XP;
        }


        Importance importanceEnumValue;
        if ("Normal - 1XP".equalsIgnoreCase(importance)) {
            importanceEnumValue = Importance.NORMAL_1XP;
        } else if ("Important - 3XP".equalsIgnoreCase(importance)) {
            importanceEnumValue = Importance.IMPORTANT_3XP;
        } else if ("Extreamly Important - 10XP".equalsIgnoreCase(importance)
                || "Extremely Important - 10XP".equalsIgnoreCase(importance)) {
            importanceEnumValue = Importance.EXTREAMLY_IMPORTANT_10XP;
        } else {
            importanceEnumValue = Importance.SPECIAL_100XP;
        }

        RepetitionUnits unitOfrepetitionEnumValue =
                "Day".equalsIgnoreCase(unitOfRepetition)
                        ? RepetitionUnits.DAY
                        : RepetitionUnits.WEEK;

        String startStr = etStartDateDatePicker.getText() != null ? etStartDateDatePicker.getText().toString() : "";
        String endStr   = etEndDateDatePicker.getText()   != null ? etEndDateDatePicker.getText().toString()   : "";

        Date repetitionStart = parseDateOrNull(startStr);
        Date repetitionEnd   = parseDateOrNull(endStr);
        boolean isOneTime = "One-time".equalsIgnoreCase(frequency);

        if (!isOneTime) {
            if (repetitionStart == null || repetitionEnd == null) {
                Toast.makeText(this, "Za ponavljajuće zadatke oba datuma su obavezna.", Toast.LENGTH_LONG).show();
                return; 
            }
            if (repetitionEnd.before(repetitionStart)) {
                Toast.makeText(this, "Krajnji datum ne može biti pre početnog.", Toast.LENGTH_LONG).show();
                return;
            }
        }
        Task newTask = new Task(
                categoryId, name, description, difficultyEnumValue,
                importanceEnumValue, frequencyEnumValue, repeatingInterval,
                unitOfrepetitionEnumValue, repetitionStart, repetitionEnd
        );


        Toast.makeText(this, testMsg, Toast.LENGTH_SHORT).show();

        taskRepository.create(newTask);
    }
}
