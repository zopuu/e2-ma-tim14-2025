package com.example.habibitar.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.habibitar.R;
import com.example.habibitar.ui.auth.LoginActivity;
import com.example.habibitar.ui.profile.ProfileActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private MaterialToolbar topAppBar;

    private static final int MENU_VIEW_PROFILE = 1001;
    private static final int MENU_LOGOUT = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        topAppBar = findViewById(R.id.topAppBar);
        topAppBar.setOnMenuItemClickListener(this::onTopBarMenuItemClick);
    }

    private boolean onTopBarMenuItemClick(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_profile) {
            showProfilePopup(topAppBar);
            return true;
        }
        return false;
    }

    private void showProfilePopup(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor, Gravity.END);
        popup.getMenu().add(0, MENU_VIEW_PROFILE, 0, "View profile");
        popup.getMenu().add(0, MENU_LOGOUT, 1, "Log out");

        popup.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == MENU_VIEW_PROFILE) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            } else if (id == MENU_LOGOUT) {
                FirebaseAuth.getInstance().signOut();
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
                Intent login = new Intent(this, LoginActivity.class);
                login.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(login);
                return true;
            }
            return false;
        });

        popup.show();
    }
}
