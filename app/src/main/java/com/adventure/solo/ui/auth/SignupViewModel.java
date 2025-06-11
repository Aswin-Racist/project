package com.adventure.solo.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class SignupViewModel extends ViewModel {

    private final FirebaseAuth auth;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false); // Init
    private final MutableLiveData<Boolean> signupSuccess = new MutableLiveData<>(false); // Init
    private final MutableLiveData<String> signupError = new MutableLiveData<>();

    @Inject
    public SignupViewModel() {
        this.auth = FirebaseAuth.getInstance();
    }

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<Boolean> getSignupSuccess() { return signupSuccess; }
    public LiveData<String> getSignupError() { return signupError; }

    public void signup(String username, String email, String password) {
        signupSuccess.setValue(false); // Reset
        signupError.setValue(null);    // Reset

        if (username == null || username.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            signupError.setValue("All fields are required.");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            signupError.setValue("Invalid email format.");
            return;
        }
        if (password.length() < 6) {
            signupError.setValue("Password must be at least 6 characters.");
            return;
        }

        isLoading.setValue(true);
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(createUserTask -> {
                if (createUserTask.isSuccessful()) {
                    FirebaseUser firebaseUser = auth.getCurrentUser();
                    if (firebaseUser != null) {
                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                .setDisplayName(username)
                                .build();
                        firebaseUser.updateProfile(profileUpdates)
                            .addOnCompleteListener(profileUpdateTask -> {
                                // Regardless of profile update result, user creation was successful.
                                signupSuccess.setValue(true);
                                isLoading.setValue(false);
                                if (!profileUpdateTask.isSuccessful()) {
                                    // Log or optionally set a non-critical error/message
                                    // For this flow, we prioritize indicating signup success.
                                    // signupError.setValue("User created, but failed to set username.");
                                }
                            });
                    } else {
                         // Should not happen if createUserTask was successful
                        signupError.setValue("User creation succeeded but user object is null.");
                        isLoading.setValue(false);
                    }
                } else {
                    signupError.setValue(createUserTask.getException() != null ? createUserTask.getException().getMessage() : "Signup failed.");
                    isLoading.setValue(false);
                }
            });
    }

    // Method to clear the error message once it's been handled by the UI
    public void onSignupErrorShown() {
        signupError.setValue(null);
    }
}
