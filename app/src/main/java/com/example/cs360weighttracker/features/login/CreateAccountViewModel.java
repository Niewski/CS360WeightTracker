package com.example.cs360weighttracker.features.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cs360weighttracker.data.UserRepository;

public class CreateAccountViewModel extends ViewModel {

    private UserRepository repository;

    private final MutableLiveData<Boolean> createResult = new MutableLiveData<>();

    public void init(UserRepository repository) {
        if (this.repository != null) return; // Already initialized
        this.repository = repository;
    }

    public LiveData<Boolean> getCreateResult() {
        return createResult;
    }

    public void createAccount(String username, String password, String goalWeightStr, String phoneNumber) {
        if (username.isEmpty() || password.isEmpty() || goalWeightStr.isEmpty()) {
            createResult.setValue(false);
            return;
        }
        try {
            double goalWeight = Double.parseDouble(goalWeightStr);
            boolean created = repository.createUser(username, password, goalWeight, phoneNumber);
            createResult.setValue(created);
        } catch (NumberFormatException e) {
            createResult.setValue(false);
        }
    }
}
