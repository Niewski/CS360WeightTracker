package com.example.cs360weighttracker.features.login;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.cs360weighttracker.R;
import com.example.cs360weighttracker.data.DatabaseHelper;

public class CreateAccountActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 123;

    EditText etUsername, etPassword, etGoalWeight, etPhoneNumber;
    Button btnCreate;
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        // Hook up layout views
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etGoalWeight = findViewById(R.id.etGoalWeight);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        btnCreate = findViewById(R.id.btnCreateAccount);

        dbHelper = new DatabaseHelper(this);

        btnCreate.setOnClickListener(v -> {
            String username = etUsername.getText().toString();
            String password = etPassword.getText().toString();
            String goalStr = etGoalWeight.getText().toString();
            String phone = etPhoneNumber.getText().toString();

            if (username.isEmpty() || password.isEmpty() || goalStr.isEmpty()) {
                Toast.makeText(this, "Username, password, and goal weight are required", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double goalWeight = Double.parseDouble(goalStr);
                boolean created = dbHelper.createUser(username, password, goalWeight, phone);
                if (created) {
                    Toast.makeText(this, "Account created.", Toast.LENGTH_SHORT).show();

                    if (!phone.isEmpty()) {
                        Toast.makeText(this, "Requesting SMS permission...", Toast.LENGTH_SHORT).show();
                        requestSmsPermission();
                    } else {
                        // Skip SMS setup
                        proceedToLogin();
                    }
                } else {
                    Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid goal weight", Toast.LENGTH_SHORT).show();
            }

        });
    }

    private void requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS},
                    SMS_PERMISSION_CODE);
        } else {
            proceedToLogin();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS permission denied. You can still use the app.", Toast.LENGTH_SHORT).show();
            }

            // In both cases, proceed to login screen
            proceedToLogin();
        }
    }

    private void proceedToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}