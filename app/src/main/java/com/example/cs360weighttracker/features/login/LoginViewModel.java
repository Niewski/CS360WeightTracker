package com.example.cs360weighttracker.features.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cs360weighttracker.data.UserRepository;

/**
 * ViewModel for the login screen.
 *
 * <p>Holds a {@link LiveData} that emits the authenticated user's ID
 * (or {@code -1} on failure). The Activity observes this to decide
 * whether to navigate forward or show an error Toast.</p>
 *
 * <h3>Initialization</h3>
 * <p>Call {@link #init(UserRepository)} once from the Activity's
 * {@code onCreate}. Subsequent calls are ignored (guard against
 * configuration changes).</p>
 *
 * @see LoginActivity
 * @see UserRepository#loginUser(String, String)
 */
public class LoginViewModel extends ViewModel {

    private UserRepository repository;

    /** Login result: {@code -1} = failed, positive value = userId. */
    private final MutableLiveData<Integer> loginResult = new MutableLiveData<>();

    /**
     * One-time initializer — supplies the repository dependency.
     *
     * @param repository repository to use for authentication
     */
    public void init(UserRepository repository) {
        if (this.repository != null) return; // Already initialized
        this.repository = repository;
    }

    /**
     * Observable login result.
     *
     * @return LiveData emitting the authenticated user ID or -1
     */
    public LiveData<Integer> getLoginResult() {
        return loginResult;
    }

    /**
     * Validates inputs and attempts authentication.
     *
     * @param username entered username
     * @param password entered password
     */
    public void login(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            loginResult.setValue(-1);
            return;
        }
        int userId = repository.loginUser(username, password);
        loginResult.setValue(userId);
    }
}
