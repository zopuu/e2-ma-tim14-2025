package com.example.habibitar.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.habibitar.R;
import com.example.habibitar.ui.main.MainActivity;
import com.example.habibitar.ui.viewmodel.AuthViewModel;
public class LoginActivity extends AppCompatActivity{
    private AuthViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        vm = new ViewModelProvider(this).get(AuthViewModel.class);

        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnResend = findViewById(R.id.btnResendEmail);
        Button btnGoRegister = findViewById(R.id.btnGoRegister);

        btnLogin.setOnClickListener(v -> vm.login(
                etEmail.getText().toString().trim(),
                etPassword.getText().toString()
        ));

        btnResend.setOnClickListener(v -> vm.resendVerification());

        btnGoRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        vm.getMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        });
        vm.getNavigateToMain().observe(this, go -> {
            if (go != null && go) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        });
    }
}
