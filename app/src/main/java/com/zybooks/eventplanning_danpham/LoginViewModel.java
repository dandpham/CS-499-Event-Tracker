package com.zybooks.eventplanning_danpham;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * LoginViewModel holds the login screen's logic. It validates input,
 * logs in or registers through UserRepository, and reports the result.
 * The login screen only displays what this provides.
 */
public class LoginViewModel extends ViewModel {

    private final UserRepository repository;

    // Data the screen observes
    private final MutableLiveData<User> loggedInUser = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public LoginViewModel(UserRepository repository) {
        this.repository = repository;
    }

    /** Set when login or registration succeeds */
    public LiveData<User> getLoggedInUser() {
        return loggedInUser;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /** True while the password is being checked, so the screen can show progress */
    public LiveData<Boolean> isLoading() {
        return loading;
    }

    public void clearError() {
        errorMessage.setValue(null);
    }

    public void login(String username, String password) {
        if (!validate(username, password)) {
            return;
        }
        loading.setValue(true);
        repository.login(username, password, resultCallback("Login failed. Please try again."));
    }

    public void register(String username, String password) {
        if (!validate(username, password)) {
            return;
        }
        loading.setValue(true);
        repository.register(username, password,
                resultCallback("Registration failed. Please try again."));
    }

    private boolean validate(String username, String password) {
        String error = Validator.checkLogin(username, password);
        if (error != null) {
            errorMessage.setValue(error);
            return false;
        }
        return true;
    }

    /**
     * Shared callback for login and registration. Expected problems, like a wrong
     * password, come back as IllegalArgumentException with a message for the user.
     * Anything else gets a general message, so no internal details reach the screen.
     */
    private RepositoryCallback<User> resultCallback(String generalError) {
        return new RepositoryCallback<User>() {
            @Override
            public void onSuccess(User result) {
                loading.setValue(false);
                loggedInUser.setValue(result);
            }

            @Override
            public void onError(Exception e) {
                loading.setValue(false);
                errorMessage.setValue(e instanceof IllegalArgumentException
                        ? e.getMessage() : generalError);
            }
        };
    }
}
