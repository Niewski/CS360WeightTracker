package com.example.cs360weighttracker.features.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cs360weighttracker.data.UserProfile;
import com.example.cs360weighttracker.data.UserRepository;

public class ProfileViewModel extends ViewModel {

    private UserRepository repository;
    private int userId = -1;

    private final MutableLiveData<UserProfile> profile = new MutableLiveData<>();
    private final MutableLiveData<Boolean> updateResult = new MutableLiveData<>();

    public void init(UserRepository repository, int userId) {
        if (this.repository != null) return; // Already initialized
        this.repository = repository;
        this.userId = userId;
        loadProfile();
    }

    public LiveData<UserProfile> getProfile() {
        return profile;
    }

    public LiveData<Boolean> getUpdateResult() {
        return updateResult;
    }

    public void loadProfile() {
        if (repository != null && userId != -1) {
            profile.setValue(repository.getUserProfile(userId));
        }
    }

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
