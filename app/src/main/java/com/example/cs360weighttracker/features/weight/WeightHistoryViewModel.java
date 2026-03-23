package com.example.cs360weighttracker.features.weight;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cs360weighttracker.data.WeightEntry;
import com.example.cs360weighttracker.data.WeightRepository;

import java.util.Collections;
import java.util.List;

public class WeightHistoryViewModel extends ViewModel {

    private WeightRepository repository;
    private int userId = -1;
    private final MutableLiveData<List<WeightEntry>> weights = new MutableLiveData<>(Collections.emptyList());

    public void init(WeightRepository repository, int userId) {
        if (this.repository != null) return; // Already initialized
        this.repository = repository;
        this.userId = userId;
        loadWeights();
    }

    public LiveData<List<WeightEntry>> getWeights() {
        return weights;
    }

    public void loadWeights() {
        if (repository != null && userId != -1) {
            weights.setValue(repository.getWeights(userId));
        }
    }

    public boolean deleteWeight(int weightId) {
        boolean deleted = repository.deleteWeight(weightId);
        if (deleted) {
            loadWeights();
        }
        return deleted;
    }
}
