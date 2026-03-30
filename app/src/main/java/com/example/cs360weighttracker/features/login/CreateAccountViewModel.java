package com.example.cs360weighttracker.features.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cs360weighttracker.data.UserRepository;

/**
 * ViewModel for the account creation screen.
 *
 * <p>Validates input and delegates account creation to {@link UserRepository}.
 * Exposes a {@link LiveData} boolean indicating success or failure.</p>
 */
public class CreateAccountViewModel extends ViewModel {

    private UserRepository repository;

    private final MutableLiveData<Boolean> createResult = new MutableLiveData<>();

    /**
     * One-time initializer — supplies the repository dependency.
     *
     * @param repository repository to use for user creation
     */
    public void init(UserRepository repository) {
        if (this.repository != null) return; // Already initialized
        this.repository = repository;
    }

    /**
     * Observable result of the create operation.
     *
     * @return LiveData emitting {@code true} on success
     */
    public LiveData<Boolean> getCreateResult() {
        return createResult;
    }

    /**
     * Attempts to create a new account after basic validation.
     *
     * @param username      entered username
     * @param password      entered password
     * @param goalWeightStr entered goal weight string; parsed to double
     * @param phoneNumber   optional phone number
     */
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
