package com.example.cs360weighttracker.features.login;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cs360weighttracker.data.UserRepository;

public class LoginViewModel extends ViewModel {

    private UserRepository repository;

    // Login result: -1 = failed, >0 = userId
    private final MutableLiveData<Integer> loginResult = new MutableLiveData<>();

    public void init(UserRepository repository) {
        if (this.repository != null) return; // Already initialized
        this.repository = repository;
    }

    public LiveData<Integer> getLoginResult() {
        return loginResult;
    }

    public void login(String username, String password) {
        if (username.isEmpty() || password.isEmpty()) {
            loginResult.setValue(-1);
            return;
        }
        int userId = repository.loginUser(username, password);
        loginResult.setValue(userId);
    }
}
