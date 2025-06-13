package com.adventure.solo.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class LoginViewModel extends ViewModel {

    private final FirebaseAuth auth;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false); // Init with false
    private final MutableLiveData<Boolean> loginSuccess = new MutableLiveData<>(false); // Init with false
    private final MutableLiveData<String> loginError = new MutableLiveData<>();

    @Inject
    public LoginViewModel() {
        this.auth = FirebaseAuth.getInstance();
    }

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getLoginSuccess() { return loginSuccess; }
    public LiveData<String> getLoginError() { return loginError; }

    public void login(String email, String password) {
        loginSuccess.setValue(false); // Reset success state
        loginError.setValue(null);    // Reset error state

        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            loginError.setValue("Email and password cannot be empty.");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            loginError.setValue("Invalid email format.");
            return;
        }

        isLoading.setValue(true);
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    loginSuccess.setValue(true);
                } else {
                    loginError.setValue(task.getException() != null ? task.getException().getMessage() : "Login failed.");
                }
                isLoading.setValue(false);
            });
    }

    // Method to clear the error message once it's been handled by the UI
    public void onLoginErrorShown() {
        loginError.setValue(null);
    }
}
