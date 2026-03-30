package com.example.cs360weighttracker.features.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cs360weighttracker.data.UserProfile;
import com.example.cs360weighttracker.data.UserRepository;

/**
 * ViewModel for the profile screen.
 *
 * <p>Loads the `UserProfile` for the provided user and exposes an
 * update operation that persists changes through {@link UserRepository}.</p>
 */
public class ProfileViewModel extends ViewModel {

    private UserRepository repository;
    private int userId = -1;

    private final MutableLiveData<UserProfile> profile = new MutableLiveData<>();
    private final MutableLiveData<Boolean> updateResult = new MutableLiveData<>();

    /**
     * One-time initializer — supplies repository and userId, then
     * loads the profile.
     */
    public void init(UserRepository repository, int userId) {
        if (this.repository != null) return; // Already initialized
        this.repository = repository;
        this.userId = userId;
        loadProfile();
    }

    /**
     * Observable profile data.
     */
    public LiveData<UserProfile> getProfile() {
        return profile;
    }

    /**
     * Observable update result.
     */
    public LiveData<Boolean> getUpdateResult() {
        return updateResult;
    }

    /**
     * Loads the current `UserProfile` from the repository.
     */
    public void loadProfile() {
        if (repository != null && userId != -1) {
            profile.setValue(repository.getUserProfile(userId));
        }
    }

    /**
     * Updates the user's goal weight and phone number.
     *
     * @param goalWeightStr goal weight as string (parsed to double)
     * @param phoneNumber   optional phone number
     */
    public void updateProfile(String goalWeightStr, String phoneNumber) {
        if (goalWeightStr.isEmpty()) {
            updateResult.setValue(false);
            return;
        }
        try {
            double goalWeight = Double.parseDouble(goalWeightStr);
            boolean updated = repository.updateUser(userId, goalWeight, phoneNumber);
            updateResult.setValue(updated);
        } catch (NumberFormatException e) {
            updateResult.setValue(false);
        }
    }
}
