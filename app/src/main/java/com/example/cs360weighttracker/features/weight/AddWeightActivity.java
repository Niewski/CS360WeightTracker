package com.example.cs360weighttracker.features.weight;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.cs360weighttracker.data.DatabaseHelper;
import com.example.cs360weighttracker.data.UserRepository;
import com.example.cs360weighttracker.data.WeightRepository;
import com.example.cs360weighttracker.R;

import java.util.Calendar;
import java.util.Locale;

/**
 * Screen for adding a new daily weight entry.
 *
 * <p>Defaults the date to today, validates input, and delegates
 * insertion to {@link AddWeightViewModel}. When a saved weight meets
 * or beats the user's goal and an SMS has not yet been sent, the
 * Activity requests SMS permission (if needed) and sends a one-time
 * notification.</p>
 */
public class AddWeightActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 2001;

    EditText etDate, etWeight;
    Button btnSave, btnCancel;
    AddWeightViewModel viewModel;
    int userId = -1;  // default/fallback
    private String pendingPhone = null;

    /**
     * Wires UI controls, initializes repositories + ViewModel, and
     * observes ViewModel LiveData for save results and goal events.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_weight);

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

        etDate.setOnClickListener(v -> {
            // Use current value in the field if parsable; otherwise, fall back to today
            Calendar initialCalendar = Calendar.getInstance();
            String currentText = etDate.getText().toString();
            if (currentText != null && !currentText.isEmpty()) {
                String[] parts = currentText.split("-");
                if (parts.length == 3) {
                    try {
                        int parsedYear = Integer.parseInt(parts[0]);
                        int parsedMonth = Integer.parseInt(parts[1]) - 1;
                        int parsedDay = Integer.parseInt(parts[2]);
                        initialCalendar.set(parsedYear, parsedMonth, parsedDay);
                    } catch (NumberFormatException ignored) {
                        // Fallback to today's date if parsing fails
                    }
                }
            }

            int initYear = initialCalendar.get(Calendar.YEAR);
            int initMonth = initialCalendar.get(Calendar.MONTH);
            int initDay = initialCalendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    AddWeightActivity.this,
                    (view, selectedYear, selectedMonth, selectedDayOfMonth) -> {
                        String selectedDate = String.format(
                                Locale.US,
                                "%04d-%02d-%02d",
                                selectedYear,
                                selectedMonth + 1,
                                selectedDayOfMonth
                        );
                        etDate.setText(selectedDate);
                    },
                    initYear,
                    initMonth,
                    initDay
            );
            datePickerDialog.show();
        });

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        WeightRepository weightRepository = new WeightRepository(dbHelper);
        UserRepository userRepository = new UserRepository(dbHelper);

        viewModel = new ViewModelProvider(this).get(AddWeightViewModel.class);
        viewModel.init(weightRepository, userRepository, userId);

        // Observe goal reached event
        viewModel.getGoalReachedEvent().observe(this, event -> {
            if (event == null || event.isHandled()) return;
            String phone = event.phoneNumber;
            event.setHandled();

            if (phone == null || phone.isEmpty()) {
                Toast.makeText(this, "Congrats on reaching your goal!", Toast.LENGTH_LONG).show();
                // Mark goal notification as handled so we don't keep showing this Toast
                viewModel.markGoalSmsSent();
                return;
            }

            pendingPhone = phone;

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                    == PackageManager.PERMISSION_GRANTED) {
                boolean sent = sendGoalReachedSms(phone);
                if (sent) viewModel.markGoalSmsSent();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
            }
        });

        // Observe save result
        viewModel.getSaveResult().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, "Weight added!", Toast.LENGTH_SHORT).show();
                finish();
            } else if (success != null) {
                Toast.makeText(this, "Insert failed", Toast.LENGTH_SHORT).show();
            }
        });

        btnSave.setOnClickListener(v -> {
            String date = etDate.getText().toString();
            String weightStr = etWeight.getText().toString();
            viewModel.saveWeight(date, weightStr);
        });

        btnCancel.setOnClickListener(v -> finish());
    }

    /**
     * Sends the goal-reached SMS using system SmsManager.
     *
     * @param phoneNumber destination phone number
     * @return {@code true} on success
     */
    private boolean sendGoalReachedSms(String phoneNumber) {
        try {
            String message = "🎉 You've reached your goal weight!";
            SmsManager smsManager = getSystemService(SmsManager.class);
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(this, "Goal reached! SMS sent.", Toast.LENGTH_SHORT).show();
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SMS.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    /**
     * Handles runtime permission result for `SEND_SMS` and attempts
     * to send the pending SMS if permission was granted.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingPhone != null) {
                    boolean sent = sendGoalReachedSms(pendingPhone);
                    if (sent) viewModel.markGoalSmsSent();
                }
            } else {
                Toast.makeText(this, "SMS permission denied. You can still use the app.", Toast.LENGTH_SHORT).show();
            }
            pendingPhone = null;
        }
    }

}
