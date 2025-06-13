package com.adventure.solo.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View; // For View.VISIBLE/GONE if using ProgressBar
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.adventure.solo.databinding.ActivityLoginBinding;
import com.adventure.solo.ui.MainActivity;
import com.google.firebase.auth.FirebaseAuth;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private LoginViewModel viewModel;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            navigateToMainApp();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        // Example: if (binding.loginProgressBar != null) binding.loginProgressBar.setVisibility(View.GONE);

        binding.loginButton.setOnClickListener(v -> {
            String email = binding.emailEditText.getText().toString().trim();
            String password = binding.passwordEditText.getText().toString().trim();
            viewModel.login(email, password);
        });

        binding.goToSignupTextView.setOnClickListener(v ->
            startActivity(new Intent(LoginActivity.this, SignupActivity.class))
        );

        observeViewModel();
    }

    private void navigateToMainApp() {
        // Ensure com.adventure.solo.ui.MainActivity is the correct class name and package
        // It was referenced as com.adventure.solo.ui.MainActivity in previous Kotlin code
        // The AndroidManifest might still point to com.demo.map.MapActivity or similar.
        // This might need reconciliation later.
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(this, isLoading -> {
            // if (binding.loginProgressBar != null) binding.loginProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.loginButton.setEnabled(!isLoading);
            binding.goToSignupTextView.setEnabled(!isLoading);
        });

        viewModel.getLoginSuccess().observe(this, isSuccess -> {
            if (isSuccess != null && isSuccess) { // Check for null for safety
                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
                navigateToMainApp();
            }
        });

        viewModel.getLoginError().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                // Optionally, you might want to clear the error in ViewModel after showing
                // viewModel.onLoginErrorShown();
            }
        });
    }
}
