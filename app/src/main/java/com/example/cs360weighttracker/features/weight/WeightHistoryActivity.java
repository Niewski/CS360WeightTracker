package com.example.cs360weighttracker.features.weight;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cs360weighttracker.data.DatabaseHelper;
import com.example.cs360weighttracker.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class WeightHistoryActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private WeightAdapter adapter;
    private int userId;
    private ArrayList<WeightEntry> weightList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight_history);

        // Get user ID from intent
        userId = getIntent().getIntExtra("userId", -1);
        if (userId == -1) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Hook up layout views
        RecyclerView recyclerView = findViewById(R.id.rvWeightEntries);
        FloatingActionButton fabAdd = findViewById(R.id.fabAddWeight);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        weightList = new ArrayList<>();

        dbHelper = new DatabaseHelper(this);

        loadWeightEntries();

        adapter = new WeightAdapter(weightList, dbHelper, this::loadWeightEntries);
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddWeightActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWeightEntries();
    }

    private void loadWeightEntries() {
        weightList.clear();
        Cursor cursor = dbHelper.getWeights(userId);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                double weight = cursor.getDouble(cursor.getColumnIndexOrThrow("weight"));
                weightList.add(new WeightEntry(id, date, weight));
            }
            cursor.close();
            if (adapter != null) adapter.notifyDataSetChanged();
        }
    }
}
