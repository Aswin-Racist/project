package com.adventure.solo.ui.auth;

import android.os.Bundle;
import android.view.View; // For View.VISIBLE/GONE if using ProgressBar
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.adventure.solo.databinding.ActivitySignupBinding;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SignupActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;
    private SignupViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(SignupViewModel.class);

        // Example: if (binding.signupProgressBar != null) binding.signupProgressBar.setVisibility(View.GONE);

        binding.signupButton.setOnClickListener(v -> {
            String username = binding.usernameEditText.getText().toString().trim();
            String email = binding.emailEditTextSignup.getText().toString().trim();
            String password = binding.passwordEditTextSignup.getText().toString().trim();
            viewModel.signup(username, email, password);
        });

        binding.goToLoginTextView.setOnClickListener(v -> finish());

        observeViewModel();
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, isLoading -> {
            // if (binding.signupProgressBar != null) binding.signupProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.signupButton.setEnabled(!isLoading);
            binding.goToLoginTextView.setEnabled(!isLoading);
        });

        viewModel.getSignupSuccess().observe(this, isSuccess -> {
            if (isSuccess != null && isSuccess) { // Check for null
                Toast.makeText(this, "Signup Successful. Please login.", Toast.LENGTH_LONG).show();
                finish();
            }
        });

        viewModel.getSignupError().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                // Optionally, clear error in ViewModel:
                // viewModel.onSignupErrorShown();
            }
        });
    }
}
