package com.example.habibitar.ui.auth;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.habibitar.R;
import com.example.habibitar.ui.viewmodel.AuthViewModel;
public class RegisterActivity extends AppCompatActivity{
    private AuthViewModel vm;

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

        Spinner spAvatar = findViewById(R.id.spAvatar);
        // simple keys to store in Firestore (maps to drawables in your app code)
        String[] avatars = {"avatar_knight","avatar_mage","avatar_archer","avatar_rogue","avatar_healer"};
        spAvatar.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, avatars));

        Button btnRegister = findViewById(R.id.btnRegister);
        Button btnGoLogin = findViewById(R.id.btnGoLogin);

        btnRegister.setOnClickListener(v -> {
            vm.register(
                    etEmail.getText().toString().trim(),
                    etPassword.getText().toString(),
                    etConfirm.getText().toString(),
                    etUsername.getText().toString().trim(),
                    spAvatar.getSelectedItem().toString()
            );
        });
        btnGoLogin.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));

        vm.getMessage().observe(this, msg ->
        { if (msg != null && !msg.isEmpty()) Toast.makeText(this, msg, Toast.LENGTH_LONG).show(); });
    }
}
