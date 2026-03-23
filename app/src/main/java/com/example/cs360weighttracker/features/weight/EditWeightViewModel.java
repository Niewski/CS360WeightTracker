package com.example.cs360weighttracker.features.weight;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cs360weighttracker.data.WeightRepository;

public class EditWeightViewModel extends ViewModel {

    private WeightRepository repository;
    private int weightId = -1;

    private final MutableLiveData<Boolean> updateResult = new MutableLiveData<>();

    public void init(WeightRepository repository, int weightId) {
        if (this.repository != null) return; // Already initialized
        this.repository = repository;
        this.weightId = weightId;
    }

    public LiveData<Boolean> getUpdateResult() {
        return updateResult;
    }

    public void updateWeight(String date, String weightStr) {
        if (date.isEmpty() || weightStr.isEmpty()) {
            updateResult.setValue(false);
            return;
        }
        try {
            double weight = Double.parseDouble(weightStr);
            boolean updated = repository.updateWeight(weightId, date, weight);
            updateResult.setValue(updated);
        } catch (NumberFormatException e) {
            updateResult.setValue(false);
        }
    }
}
