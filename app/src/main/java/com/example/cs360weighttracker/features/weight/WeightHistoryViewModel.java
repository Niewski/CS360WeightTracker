package com.example.cs360weighttracker.features.weight;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cs360weighttracker.data.WeightEntry;
import com.example.cs360weighttracker.data.WeightRepository;

import java.util.Collections;
import java.util.List;

/**
 * ViewModel for the weight history screen.
 *
 * <p>Exposes a {@link LiveData} list of {@link WeightEntry} and provides
 * methods to load and delete entries. The ViewModel holds the
 * `userId` and a reference to {@link WeightRepository}.</p>
 */
public class WeightHistoryViewModel extends ViewModel {

    private WeightRepository repository;
    private int userId = -1;
    private final MutableLiveData<List<WeightEntry>> weights = new MutableLiveData<>(Collections.emptyList());

    /**
     * One-time initializer — supplies repository and user ID, then
     * loads the initial dataset.
     *
     * @param repository repository for weight data
     * @param userId     the owning user's primary-key ID
     */
    public void init(WeightRepository repository, int userId) {
        if (this.repository != null) return; // Already initialized
        this.repository = repository;
        this.userId = userId;
        loadWeights();
    }

    /**
     * Observable list of weight entries.
     *
     * @return LiveData emitting the user's weights
     */
    public LiveData<List<WeightEntry>> getWeights() {
        return weights;
    }

    /**
     * Loads the weights from the repository and updates the LiveData.
     */
    public void loadWeights() {
        if (repository != null && userId != -1) {
            weights.setValue(repository.getWeights(userId));
        }
    }

    /**
     * Deletes a weight entry and refreshes the list on success.
     *
     * @param weightId primary-key ID of the weight row to delete
     * @return {@code true} if deletion succeeded
     */
    public boolean deleteWeight(int weightId) {
        if (repository == null || userId == -1) {
            return false;
        }
        boolean deleted = repository.deleteWeight(weightId);
        if (deleted) {
            loadWeights();
        }
        return deleted;
    }
}
