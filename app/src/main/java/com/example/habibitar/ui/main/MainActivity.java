package com.example.habibitar.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.habibitar.R;
import com.example.habibitar.ui.auth.LoginActivity;
import com.example.habibitar.ui.profile.ProfileActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity implements com.google.android.material.navigation.NavigationView.OnNavigationItemSelectedListener {

    private MaterialToolbar topAppBar;
    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawer;

    private static final int MENU_VIEW_PROFILE = 1001;
    private static final int MENU_LOGOUT = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        drawer = findViewById(R.id.drawer_layout);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        topAppBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topAppBar);

        // Drawer setup
        androidx.drawerlayout.widget.DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);

        drawerToggle = new ActionBarDrawerToggle(
                this, drawer, topAppBar,
                R.string.nav_open, R.string.nav_close
        );
        drawer.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        navView.setNavigationItemSelectedListener(this);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                } else {
                    // temporarily disable this callback and let the system handle back
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        //topAppBar.setOnMenuItemClickListener(this::onTopBarMenuItemClick);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the right-side profile menu for the ActionBar
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Let the drawer toggle handle the hamburger first
        if (drawerToggle != null && drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        if (item.getItemId() == R.id.action_profile) {
            showProfilePopup(topAppBar); // your existing popup
            return true;
        }
        return super.onOptionsItemSelected(item);
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
    // Handle drawer item clicks
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_friends) {
            startActivity(new Intent(this, com.example.habibitar.ui.friends.FriendsActivity.class));
        }
        if (id == R.id.nav_alliance) {
            startActivity(new Intent(this, com.example.habibitar.ui.alliance.AllianceActivity.class));
        }

        // Close drawer after click
        androidx.drawerlayout.widget.DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /* depreciated
    @Override
    public void onBackPressed() {
        androidx.drawerlayout.widget.DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    */
}
