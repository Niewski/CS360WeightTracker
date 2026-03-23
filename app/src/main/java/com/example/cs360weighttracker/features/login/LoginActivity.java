package com.example.cs360weighttracker.features.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.cs360weighttracker.data.DatabaseHelper;
import com.example.cs360weighttracker.data.UserRepository;
import com.example.cs360weighttracker.R;
import com.example.cs360weighttracker.features.weight.WeightHistoryActivity;

public class LoginActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnLogin, btnCreateAccount;
    LoginViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Hook up layout views
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        UserRepository userRepository = new UserRepository(dbHelper);

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        viewModel.init(userRepository);

        viewModel.getLoginResult().observe(this, userId -> {
            if (userId != null && userId != -1) {
                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, WeightHistoryActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            } else if (userId != null) {
                Toast.makeText(this, "Invalid login", Toast.LENGTH_SHORT).show();
            }
        });

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString();
            String password = etPassword.getText().toString();
            viewModel.login(username, password);
        });

        btnCreateAccount.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, CreateAccountActivity.class);
            startActivity(intent);
        });
    }
}
