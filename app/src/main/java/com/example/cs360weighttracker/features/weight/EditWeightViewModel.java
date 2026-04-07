package com.example.cs360weighttracker.features.weight;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cs360weighttracker.data.WeightRepository;

/**
 * ViewModel for the "edit weight" screen.
 *
 * <p>Holds the target weight row ID and exposes methods to update the
 * row. Update results are published via {@link LiveData} so the
 * Activity can react.</p>
 */
public class EditWeightViewModel extends ViewModel {

    private WeightRepository repository;
    private int weightId = -1;

    private final MutableLiveData<Boolean> updateResult = new MutableLiveData<>();

    /**
     * One-time initializer supplying the repository and the weightId.
     */
    public void init(WeightRepository repository, int weightId) {
        if (this.repository != null) return; // Already initialized
        this.repository = repository;
        this.weightId = weightId;
    }

    /**
     * Observable update result.
     */
    public LiveData<Boolean> getUpdateResult() {
        return updateResult;
    }

    /**
     * Validates input and requests an update from the repository.
     *
     * @param date      new date in YYYY-MM-DD format
     * @param weightStr new weight string to parse as double
     */
    public void updateWeight(String date, String weightStr, String notes) {
        if (date.isEmpty() || weightStr.isEmpty()) {
            updateResult.setValue(false);
            return;
        }
        try {
            double weight = Double.parseDouble(weightStr);
            boolean updated = repository.updateWeight(weightId, date, weight, notes);
            updateResult.setValue(updated);
        } catch (NumberFormatException e) {
            updateResult.setValue(false);
        }
    }
}
