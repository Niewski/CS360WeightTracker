package com.example.cs360weighttracker.features.weight;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.cs360weighttracker.data.DatabaseHelper;
import com.example.cs360weighttracker.R;

import java.util.Calendar;
import java.util.Locale;

public class AddWeightActivity extends AppCompatActivity {

    EditText etDate, etWeight;
    Button btnSave, btnCancel;
    DatabaseHelper dbHelper;
    int userId = -1;  // default/fallback

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_weight);

        dbHelper = new DatabaseHelper(this);

        // Get user ID from intent
        userId = getIntent().getIntExtra("userId", -1);
        if (userId == -1) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Hook up layout views
        etDate = findViewById(R.id.etDate);
        etWeight = findViewById(R.id.etWeight);
        btnSave = findViewById(R.id.btnSaveWeight);
        btnCancel = findViewById(R.id.btnCancel);

        // Default to today’s date
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String formattedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day);
        etDate.setText(formattedDate);

        // Date picker on click
        etDate.setOnClickListener(v -> {
            DatePickerDialog dpd = new DatePickerDialog(AddWeightActivity.this,
                    (view, y, m, d) -> etDate.setText(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)),
                    year, month, day);
            dpd.show();
        });

        // Save weight to database
        btnSave.setOnClickListener(v -> {
            String date = etDate.getText().toString();
            String weightStr = etWeight.getText().toString();

            if (date.isEmpty() || weightStr.isEmpty()) {
                Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double weight = Double.parseDouble(weightStr);
                boolean inserted = dbHelper.addWeight(userId, date, weight);
                if (inserted) {
                    double goalWeight = dbHelper.getGoalWeight(userId);

                    if (weight <= goalWeight) {
                        // We do not want to send message every time user submits weight at/under goal weight
                        if (!dbHelper.isGoalSmsSent(userId)) {
                            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                                    == PackageManager.PERMISSION_GRANTED) {
                                String phoneNumber = dbHelper.getPhoneNumber(userId);
                                sendGoalReachedSms(phoneNumber);
                            }
                            // Set to true, even if no actual SMS was sent. User doesn't need a notification of goal met every time
                            dbHelper.setGoalSmsSent(userId);

                            Toast.makeText(this, "Congrats on reaching your goal!", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }

                    Toast.makeText(this, "Weight added!", Toast.LENGTH_SHORT).show();
                    finish();
                }

                Toast.makeText(this, "Insert failed", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid weight format", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> finish());
    }

    private void sendGoalReachedSms(String phoneNumber) {
        String message = "🎉 You've reached your goal weight!";
        SmsManager smsManager;

        smsManager = getSystemService(SmsManager.class);

        smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        Toast.makeText(this, "Goal reached! SMS sent.", Toast.LENGTH_SHORT).show();
    }

}
