// CategoriesActivity.java
package com.example.habibitar.ui.Task.Categories;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.habibitar.R;
import com.example.habibitar.data.category.CategoryRepository;
import com.example.habibitar.domain.model.Category;
import com.example.habibitar.ui.profile.CategoryAdapter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CategoriesActivity extends AppCompatActivity {

    private EditText etCategoryName;
    private View preview;
    private LinearLayout colorStrip;

    private RecyclerView rv;
    private CategoryAdapter adapter;

    private CategoryRepository categoryRepository;
    private final List<View> swatches = new ArrayList<>();
    private int selectedColor = Color.TRANSPARENT;

    private final List<Category> loadedCategories = new ArrayList<>();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.categories);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        etCategoryName = findViewById(R.id.etCategoryName);
        preview        = findViewById(R.id.viewColorPreview);
        colorStrip     = findViewById(R.id.llColorStrip);

        categoryRepository = new CategoryRepository();

        populateColorStrip(); 

        rv = findViewById(R.id.rvCategories);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CategoryAdapter(item -> showEditColorDialog(item));
        rv.setAdapter(adapter);

        loadCategories();
    }

    private void loadCategories() {
        categoryRepository.getAllForCurrentUser()
                .thenAccept(categories -> runOnUiThread(() -> {
                    loadedCategories.clear();
                    loadedCategories.addAll(categories); 
                    adapter.setItems(categories);
                    populateColorStrip();
                }))
                .exceptionally(ex -> {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Load failed: " + ex.getMessage(), Toast.LENGTH_LONG).show()
                    );
                    return null;
                });
    }

    private java.util.Set<String> getUsedColorsUpper() {
        java.util.Set<String> used = new java.util.HashSet<>();
        for (Category c : loadedCategories) {
            if (c.getColorCode() != null) {
                used.add(c.getColorCode().toUpperCase());
            }
        }
        return used;
    }


    private void populateColorStrip() {
        
        colorStrip.removeAllViews();
        swatches.clear();

        TypedArray colors = getResources().obtainTypedArray(R.array.category_colors);
        Set<String> used = getUsedColorsUpper(); 

        final int count  = colors.length();
        final int size   = dp(36);
        final int margin = dp(6);

        View firstSelectable = null;

        for (int i = 0; i < count; i++) {
            final int color = colors.getColor(i, Color.BLACK);
            final String hex = String.format("#%06X", (0xFFFFFF & color)).toUpperCase();

            if (used.contains(hex)) continue;

            View swatch = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(margin, margin, margin, margin);
            swatch.setLayoutParams(lp);

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(dp(8));
            bg.setColor(color);
            bg.setStroke(dp(1), 0x66000000);
            swatch.setBackground(bg);

            swatch.setTag(color);

            swatch.setOnClickListener(v -> selectColor(v, color));

            colorStrip.addView(swatch);
            swatches.add(swatch);

            if (firstSelectable == null) firstSelectable = swatch;
        }

        colors.recycle();

        if (firstSelectable != null) {
            int firstColor = (int) firstSelectable.getTag();
            selectColor(firstSelectable, firstColor);
        } else {
            preview.setBackgroundColor(0xFFEEEEEE);
            Toast.makeText(this, "No available colors. Change some existing category color first.", Toast.LENGTH_SHORT).show();
        }
    }


    private void selectColor(View swatch, int color) {
        selectedColor = color;
        GradientDrawable pbg = new GradientDrawable();
        pbg.setShape(GradientDrawable.RECTANGLE);
        pbg.setCornerRadius(dp(8));
        pbg.setColor(color);
        pbg.setStroke(dp(1), 0x66000000);
        preview.setBackground(pbg);

        for (View v : swatches) {
            GradientDrawable d = (GradientDrawable) v.getBackground();
            if (v == swatch) d.setStroke(dp(3), 0xFF000000);
            else d.setStroke(dp(1), 0x66000000);
        }
    }

    public void onSaveClicked(View v) {
        if (swatches.isEmpty()) {
            Toast.makeText(this, "No available colors to assign.", Toast.LENGTH_SHORT).show();
            return;
        }
        String codeForColour = toHexRGB(selectedColor);
        String name = safeText(etCategoryName);
        if (name.isEmpty()) {
            etCategoryName.setError("Name is required");
            return;
        }

        categoryRepository.create(name, codeForColour)
                .thenAccept(cat -> runOnUiThread(() -> {
                    Toast.makeText(this, "Saved: " + cat.getName(), Toast.LENGTH_SHORT).show();
                    loadCategories(); 
                }))
                .exceptionally(ex -> { runOnUiThread(() ->
                        Toast.makeText(this, "Save failed: " + ex.getMessage(), Toast.LENGTH_LONG).show()
                ); return null; });
    }


    private void showEditColorDialog(Category category) {
        View content = LayoutInflater.from(this)
                .inflate(R.layout.dialog_edit_category_color, null, false);

        ((android.widget.TextView) content.findViewById(R.id.tvCategoryNameDialog))
                .setText(category.getName());

        View preview = content.findViewById(R.id.viewSelectedColorPreview);
        LinearLayout llColors = content.findViewById(R.id.llColorsDialog);

        Set<String> usedColors = new HashSet<>();
        for (Category c : loadedCategories) {
            if (c.getColorCode() != null) usedColors.add(c.getColorCode().toUpperCase());
        }
        if (category.getColorCode() != null) {
            usedColors.remove(category.getColorCode().toUpperCase());
        }

        final int[] selected = { parseColorSafe(category.getColorCode(), Color.LTGRAY) };
        buildColorRow(llColors, preview, selected, usedColors);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Change color")
                .setView(content)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Save", (d, w) -> {
                    String hex = toHexRGB(selected[0]);
                    categoryRepository.updateColor(category.getId(), hex)
                            .thenAccept(v -> runOnUiThread(() -> {
                                Toast.makeText(this, "Color updated", Toast.LENGTH_SHORT).show();
                                loadCategories();
                            }))
                            .exceptionally(ex -> { runOnUiThread(() ->
                                    Toast.makeText(this, "Update failed: " + ex.getMessage(), Toast.LENGTH_LONG).show()
                            ); return null; });
                })
                .show();
    }

    private void buildColorRow(LinearLayout container, View preview, int[] selected, Set<String> used) {
        container.removeAllViews();
        TypedArray colors = getResources().obtainTypedArray(R.array.category_colors);
        int size = dp(36), margin = dp(6);

        for (int i = 0; i < colors.length(); i++) {
            final int color = colors.getColor(i, Color.BLACK);
            final String hex = toHexRGB(color).toUpperCase();

            if (used.contains(hex)) continue; 

            View sw = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(margin, margin, margin, margin);
            sw.setLayoutParams(lp);

            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(dp(8));
            bg.setColor(color);
            bg.setStroke(dp(1), 0x66000000);
            sw.setBackground(bg);

            if (colorEquals(color, selected[0])) {
                bg.setStroke(dp(3), 0xFF000000);
                setPreview(preview, color);
            }

            sw.setOnClickListener(v -> {
                selected[0] = color;
                setPreview(preview, color);
                for (int j = 0; j < container.getChildCount(); j++) {
                    GradientDrawable g = (GradientDrawable) container.getChildAt(j).getBackground();
                    g.setStroke(dp(1), 0x66000000);
                }
                ((GradientDrawable) sw.getBackground()).setStroke(dp(3), 0xFF000000);
            });

            container.addView(sw);
        }
        colors.recycle();
    }

    private void setPreview(View preview, int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setCornerRadius(dp(8));
        d.setColor(color);
        d.setStroke(dp(1), 0x66000000);
        preview.setBackground(d);
    }

    private String safeText(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }
    private int dp(int v) { return Math.round(getResources().getDisplayMetrics().density * v); }
    private static int parseColorSafe(String hex, int fallback) {
        try { return Color.parseColor(hex); } catch (Exception e) { return fallback; }
    }
    private static boolean colorEquals(int c1, int c2) {
        return (c1 & 0xFFFFFF) == (c2 & 0xFFFFFF);
    }
    private static String toHexRGB(int colorInt) {
        return String.format("#%06X", (0xFFFFFF & colorInt));
    }
}
