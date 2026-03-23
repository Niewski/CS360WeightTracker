package com.example.cs360weighttracker.features.weight;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cs360weighttracker.R;
import com.example.cs360weighttracker.data.DatabaseHelper;
import com.example.cs360weighttracker.data.WeightEntry;
import com.example.cs360weighttracker.data.WeightRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class WeightHistoryActivity extends AppCompatActivity {

    private WeightAdapter adapter;
    private int userId;
    private final ArrayList<WeightEntry> weightList = new ArrayList<>();
    private WeightHistoryViewModel viewModel;

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

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        WeightRepository repository = new WeightRepository(dbHelper);

        viewModel = new ViewModelProvider(this).get(WeightHistoryViewModel.class);
        viewModel.init(repository, userId);

        adapter = new WeightAdapter(weightList,
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
                    startActivity(intent);
                }
        );

        recyclerView.setAdapter(adapter);

        viewModel.getWeights().observe(this, weights -> {
            weightList.clear();
            if (weights != null) weightList.addAll(weights);
            adapter.notifyDataSetChanged();
        });

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddWeightActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, com.example.cs360weighttracker.features.profile.ProfileActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.loadWeights();
    }
}
