package com.example.cs360weighttracker.features.profile;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.cs360weighttracker.R;
import com.example.cs360weighttracker.data.DatabaseHelper;
import com.example.cs360weighttracker.data.UserRepository;

import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    EditText etUsername, etGoalWeight, etPhoneNumber;
    Button btnSave, btnCancel;
    ProfileViewModel viewModel;
    int userId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Get user ID from intent
        userId = getIntent().getIntExtra("userId", -1);
        if (userId == -1) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Hook up layout views
        etUsername = findViewById(R.id.etUsername);
        etGoalWeight = findViewById(R.id.etGoalWeight);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        btnSave = findViewById(R.id.btnSaveProfile);
        btnCancel = findViewById(R.id.btnCancel);

        // Set up ViewModel
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        UserRepository userRepository = new UserRepository(dbHelper);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        viewModel.init(userRepository, userId);

        // Observe profile data
        viewModel.getProfile().observe(this, profile -> {
            if (profile != null) {
                etUsername.setText(profile.username);
                etGoalWeight.setText(String.format(Locale.US, "%.1f", profile.goalWeight));
                if (profile.phoneNumber != null) {
                    etPhoneNumber.setText(profile.phoneNumber);
                }
            }
        });

        // Observe update result
        viewModel.getUpdateResult().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
                finish();
            } else if (success != null) {
                Toast.makeText(this, "Update failed or invalid input", Toast.LENGTH_SHORT).show();
            }
        });

        btnSave.setOnClickListener(v -> {
            String goalStr = etGoalWeight.getText().toString();
            String phone = etPhoneNumber.getText().toString();
            viewModel.updateProfile(goalStr, phone);
        });

        btnCancel.setOnClickListener(v -> finish());
    }
}
