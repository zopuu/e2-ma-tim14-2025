package com.example.habibitar.ui.friends;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.habibitar.R;

public class FriendsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.friends_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.menu_friends);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }
}
