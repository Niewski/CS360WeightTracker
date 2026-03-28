package com.example.cs360weighttracker.features.weight;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.cs360weighttracker.R;
import com.example.cs360weighttracker.data.DatabaseHelper;
import com.example.cs360weighttracker.data.WeightRepository;

import java.util.Calendar;
import java.util.Locale;

/**
 * Screen for editing an existing weight entry.
 *
 * <p>Pre-populates fields from Intent extras, allows the user to
 * adjust date and weight, and updates the database via
 * {@link EditWeightViewModel}.</p>
 */
public class EditWeightActivity extends AppCompatActivity {

    EditText etDate, etWeight;
    Button btnUpdate, btnCancel;
    EditWeightViewModel viewModel;
    int userId = -1;
    int weightId = -1;

    /**
     * Initializes UI, sets up the ViewModel, and observes the update
     * result to finish the Activity on success.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_weight);

        // Get extras from intent
        userId = getIntent().getIntExtra("userId", -1);
        weightId = getIntent().getIntExtra("weightId", -1);
        String existingDate = getIntent().getStringExtra("date");
        double existingWeight = getIntent().getDoubleExtra("weight", 0.0);

        if (userId == -1 || weightId == -1) {
            Toast.makeText(this, "Invalid entry", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Hook up layout views
        etDate = findViewById(R.id.etDate);
        etWeight = findViewById(R.id.etWeight);
        btnUpdate = findViewById(R.id.btnUpdateWeight);
        btnCancel = findViewById(R.id.btnCancel);

        // Set up ViewModel
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        WeightRepository repository = new WeightRepository(dbHelper);
        viewModel = new ViewModelProvider(this).get(EditWeightViewModel.class);
        viewModel.init(repository, weightId);

        // Pre-populate with existing values
        etDate.setText(existingDate);
        etWeight.setText(String.format(Locale.US, "%.1f", existingWeight));

        // Date picker on click
        Calendar calendar = Calendar.getInstance();
        etDate.setOnClickListener(v -> {
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            if (existingDate != null && existingDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                String[] parts = existingDate.split("-");
                year = Integer.parseInt(parts[0]);
                month = Integer.parseInt(parts[1]) - 1;
                day = Integer.parseInt(parts[2]);
            }
            DatePickerDialog dpd = new DatePickerDialog(EditWeightActivity.this,
                    (view, y, m, d) -> etDate.setText(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)),
                    year, month, day);
            dpd.show();
        });

        // Observe update result
        viewModel.getUpdateResult().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, "Weight updated!", Toast.LENGTH_SHORT).show();
                finish();
            } else if (success != null) {
                Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
            }
        });

        btnUpdate.setOnClickListener(v -> {
            String date = etDate.getText().toString();
            String weightStr = etWeight.getText().toString();
            viewModel.updateWeight(date, weightStr);
        });

        btnCancel.setOnClickListener(v -> finish());
    }
}
