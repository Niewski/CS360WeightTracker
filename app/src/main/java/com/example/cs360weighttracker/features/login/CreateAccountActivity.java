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
import androidx.lifecycle.ViewModelProvider;

import com.example.cs360weighttracker.R;
import com.example.cs360weighttracker.data.DatabaseHelper;
import com.example.cs360weighttracker.data.UserRepository;

/**
 * Account creation screen.
 *
 * <p>Allows creating a new user with an optional phone number. If a
 * phone number is provided, the Activity requests `SEND_SMS` permission
 * so the app can later send a goal-reached SMS.</p>
 */
public class CreateAccountActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 123;

    EditText etUsername, etPassword, etGoalWeight, etPhoneNumber;
    Button btnCreate;
    CreateAccountViewModel viewModel;

    /**
     * Inflates the layout, initializes the ViewModel, and observes the
     * account-creation result to prompt for SMS permission or navigate.
     */
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

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        UserRepository userRepository = new UserRepository(dbHelper);

        viewModel = new ViewModelProvider(this).get(CreateAccountViewModel.class);
        viewModel.init(userRepository);

        viewModel.getCreateResult().observe(this, created -> {
            if (created != null && created) {
                Toast.makeText(this, "Account created.", Toast.LENGTH_SHORT).show();
                String phone = etPhoneNumber.getText().toString();
                if (!phone.isEmpty()) {
                    requestSmsPermission();
                } else {
                    proceedToLogin();
                }
            } else if (created != null) {
                Toast.makeText(this, "Username already exists or invalid input", Toast.LENGTH_SHORT).show();
            }
        });

        btnCreate.setOnClickListener(v -> {
            String username = etUsername.getText().toString();
            String password = etPassword.getText().toString();
            String goalStr = etGoalWeight.getText().toString();
            String phone = etPhoneNumber.getText().toString();
            viewModel.createAccount(username, password, goalStr, phone);
        });
    }

    /**
     * Requests `SEND_SMS` permission if not already granted; otherwise
     * proceeds to the login screen.
     */
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

    /**
     * Handles the SMS permission result and always navigates to the
     * login screen afterwards.
     */
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

    /**
     * Navigates to the login screen and finishes this Activity.
     */
    private void proceedToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}