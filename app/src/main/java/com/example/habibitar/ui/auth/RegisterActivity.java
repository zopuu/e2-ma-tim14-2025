package com.example.habibitar.ui.auth;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.habibitar.R;
import com.example.habibitar.ui.viewmodel.AuthViewModel;

import java.util.Arrays;

public class RegisterActivity extends AppCompatActivity{
    private AuthViewModel vm;
    private String selectedAvatarKey = null;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        vm = new ViewModelProvider(this).get(AuthViewModel.class);

        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        EditText etConfirm = findViewById(R.id.etConfirm);
        EditText etUsername = findViewById(R.id.etUsername);

        // --- avatars grid setup ---
        androidx.recyclerview.widget.RecyclerView rv = findViewById(R.id.rvAvatars);
        rv.setLayoutManager(new GridLayoutManager(this, 3));
        AvatarsAdapter adapter = new AvatarsAdapter(opt -> selectedAvatarKey = opt.key);
        rv.setAdapter(adapter);

        // Build the 5 options (keys must match Avatars.map keys)
        adapter.submit(Arrays.asList(
                new AvatarsAdapter.AvatarOption("a01", R.drawable.elephant),
                new AvatarsAdapter.AvatarOption("a02", R.drawable.eagle),
                new AvatarsAdapter.AvatarOption("a03", R.drawable.chameleon),
                new AvatarsAdapter.AvatarOption("a04", R.drawable.giraffe),
                new AvatarsAdapter.AvatarOption("a05", R.drawable.squirrel)
        ));

        Button btnRegister = findViewById(R.id.btnRegister);
        Button btnGoLogin = findViewById(R.id.btnGoLogin);

        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass  = etPassword.getText().toString();
            String conf  = etConfirm.getText().toString();
            String user  = etUsername.getText().toString().trim();

            if (selectedAvatarKey == null) {
                Toast.makeText(this, "Please pick an avatar", Toast.LENGTH_SHORT).show();
                return;
            }
            vm.register(email, pass, conf, user, selectedAvatarKey);
        });
        btnGoLogin.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));

        vm.getMessage().observe(this, msg ->
        { if (msg != null && !msg.isEmpty()) Toast.makeText(this, msg, Toast.LENGTH_LONG).show(); });
    }
}
