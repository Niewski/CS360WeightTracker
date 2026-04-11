package com.example.cs360weighttracker.features.weight;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cs360weighttracker.R;
import com.example.cs360weighttracker.data.DatabaseHelper;
import com.example.cs360weighttracker.data.DataExporter;
import com.example.cs360weighttracker.data.DataImporter;
import com.example.cs360weighttracker.data.WeightEntry;
import com.example.cs360weighttracker.data.WeightRepository;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.util.Date;
/**
 * Main screen of the application — displays the logged-in user's weight
 * history as a scrollable list.
 *
 * <p>Hosts a {@link RecyclerView} backed by {@link WeightAdapter} and
 * exposes actions to add, edit, or delete entries. The Activity
 * observes {@link WeightHistoryViewModel#getWeights()} and refreshes
 * the list on changes. The {@code userId} must be provided via the
 * launching Intent.</p>
 */
public class WeightHistoryActivity extends AppCompatActivity {

    private WeightAdapter weightAdapter;
    private SummaryAdapter summaryAdapter;
    private int userId;
    private final ArrayList<WeightEntry> weightList = new ArrayList<>();
    private WeightHistoryViewModel viewModel;
    private Spinner spinnerMode;

    private DatabaseHelper dbHelper;
    /**
     * Wires UI, creates the repository + ViewModel, and observes the
     * ViewModel's LiveData to render weight entries.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight_history);

        userId = getIntent().getIntExtra("userId", -1);
        if (userId == -1) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        RecyclerView recyclerView = findViewById(R.id.rvWeightEntries);
        FloatingActionButton fabAdd = findViewById(R.id.fabAddWeight);
        Button btnProfile = findViewById(R.id.btnProfile);
        Button btnAnalytics = findViewById(R.id.btnAnalytics);
        EditText etSearch = findViewById(R.id.etSearch);
        Button btnDateRange = findViewById(R.id.btnDateRange);
        Button btnClearFilter = findViewById(R.id.btnClearFilter);
        spinnerMode = findViewById(R.id.spinnerMode);
        Button btnExport = findViewById(R.id.btnExport);
        Button btnImport = findViewById(R.id.btnImport);
        TextView tvHeaderDate = findViewById(R.id.tvHeaderDate);
        TextView tvHeaderWeight = findViewById(R.id.tvHeaderWeight);
        TextView tvHeaderAction = findViewById(R.id.tvHeaderAction);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        dbHelper = new DatabaseHelper(this);
        WeightRepository repository = new WeightRepository(dbHelper);

        viewModel = new ViewModelProvider(this).get(WeightHistoryViewModel.class);
        viewModel.init(repository, userId);

        weightAdapter = new WeightAdapter(weightList,
                weightId -> {
                    boolean deleted = viewModel.deleteWeight(weightId);
                    if (deleted) Toast.makeText(this, "Entry deleted", Toast.LENGTH_SHORT).show();
                    else Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show();
                },
                entry -> {
                    Intent intent = new Intent(this, EditWeightActivity.class);
                    intent.putExtra("userId", userId);
                    intent.putExtra("weightId", entry.id);
                    intent.putExtra("date", entry.date);
                    intent.putExtra("weight", entry.weight);
                    intent.putExtra("notes", entry.notes);
                    startActivity(intent);
                }
        );

        summaryAdapter = new SummaryAdapter();

        recyclerView.setAdapter(weightAdapter);

        viewModel.getWeights().observe(this, weights -> {
            weightList.clear();
            if (weights != null) weightList.addAll(weights);
            // ensure weightAdapter is active
            if (recyclerView.getAdapter() != weightAdapter) recyclerView.setAdapter(weightAdapter);
            weightAdapter.notifyDataSetChanged();
        });

        viewModel.getSummaryData().observe(this, summary -> {
            if (summary == null) return;
            summaryAdapter.setData(summary);
            if (recyclerView.getAdapter() != summaryAdapter) recyclerView.setAdapter(summaryAdapter);
        });

        // Spinner setup
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(this,
            R.array.summary_modes, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMode.setAdapter(spinnerAdapter);
        spinnerMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.setDisplayMode(position);
                if (position == 0) {
                    tvHeaderDate.setText(R.string.date_label);
                    tvHeaderWeight.setText(R.string.weight_label);
                    tvHeaderAction.setText(R.string.action_label);
                } else {
                    tvHeaderDate.setText(R.string.period_label);
                    tvHeaderWeight.setText(R.string.avg_weight_label);
                    tvHeaderAction.setText(R.string.entries_label);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Search debounce — reset spinner to mode 0 when searching
        final android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        final Runnable[] searchRunnable = new Runnable[1];
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (searchRunnable[0] != null) searchHandler.removeCallbacks(searchRunnable[0]);
                searchRunnable[0] = () -> {
                    if (spinnerMode.getSelectedItemPosition() != 0) {
                        spinnerMode.setSelection(0);
                    }
                    viewModel.searchNotes(s.toString().trim());
                };
                searchHandler.postDelayed(searchRunnable[0], 300);
            }
        });

        // Date range picker — reset spinner to mode 0 when filtering
        btnDateRange.setOnClickListener(v -> {
            MaterialDatePicker<Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker().build();
            picker.show(getSupportFragmentManager(), "date_range_picker");
            picker.addOnPositiveButtonClickListener(selection -> {
                Long start = selection.first;
                Long end = selection.second;
                if (start != null && end != null) {
                    if (spinnerMode.getSelectedItemPosition() != 0) {
                        spinnerMode.setSelection(0);
                    }
                    viewModel.loadWeightsInRange(formatMillisToDate(start), formatMillisToDate(end));
                }
            });
        });

        btnClearFilter.setOnClickListener(v -> {
            etSearch.setText("");
            viewModel.loadWeights();
        });

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddWeightActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        btnAnalytics.setOnClickListener(v -> {
            Intent intent = new Intent(this, AnalyticsActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.example.cs360weighttracker.features.profile.ProfileActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        // Export/import handlers
        final ActivityResultLauncher<Intent> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                            DataExporter.exportToCsv(userId, os, dbHelper);
                            Toast.makeText(this, R.string.export_success, Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        );

        final ActivityResultLauncher<Intent> importLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        try (InputStream is = getContentResolver().openInputStream(uri)) {
                            int imported = DataImporter.importFromCsv(userId, is, dbHelper);
                            Toast.makeText(this, getResources().getQuantityString(R.plurals.import_count, imported, imported), Toast.LENGTH_LONG).show();
                            viewModel.loadWeights();
                        } catch (IOException e) {
                            Toast.makeText(this, R.string.import_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        );

        btnExport.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_TITLE, "weights_export.csv");
            exportLauncher.launch(intent);
        });

        btnImport.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "text/csv",
                "text/comma-separated-values",
                "application/csv",
                "application/vnd.ms-excel"
            });
            importLauncher.launch(intent);
        });
    }

    private String formatMillisToDate(Long millis) {
        if (millis == null) return null;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(millis));
    }

    /**
     * Ensures the latest data is loaded when returning to this
     * Activity (e.g., after Add/Edit operations), respecting the
     * currently selected display mode.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            Integer mode = viewModel.getDisplayMode().getValue();
            viewModel.setDisplayMode(mode != null ? mode : 0);
        }
    }
}
