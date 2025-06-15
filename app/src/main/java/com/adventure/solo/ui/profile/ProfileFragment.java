package com.adventure.solo.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
// import android.widget.TextView; // Not strictly needed if not finding view by id for title
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

// import com.adventure.solo.R; // Not strictly needed for this version
import com.adventure.solo.databinding.DialogProfileBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.adventure.solo.model.PlayerProfile;

import dagger.hilt.android.AndroidEntryPoint;
import android.util.Log;

@AndroidEntryPoint
public class ProfileFragment extends Fragment {
    private static final String TAG = "ProfileFragment";

    private ProfileViewModel viewModel;
    private DialogProfileBinding binding;
    private FirebaseAuth auth;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogProfileBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        auth = FirebaseAuth.getInstance();

        setupUIListeners();
        observeViewModel();

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Calling refreshUserProfile");
        viewModel.refreshUserProfile(); // Corrected to use the public method in ViewModel
    }

    private void setupUIListeners() {
        binding.createTeamButton.setOnClickListener(v -> {
            if (binding.teamNameEditText.getText() != null) {
                String teamName = binding.teamNameEditText.getText().toString().trim();
                viewModel.createTeam(teamName);
            }
        });

        binding.joinTeamButton.setOnClickListener(v -> {
            if (binding.joinTeamIdEditText.getText() != null) {
                String teamId = binding.joinTeamIdEditText.getText().toString().trim();
                viewModel.joinTeam(teamId);
            }
        });

        binding.leaveTeamButton.setOnClickListener(v -> viewModel.leaveTeam());
    }

    private void observeViewModel() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                binding.createTeamButton.setEnabled(!isLoading);
                binding.joinTeamButton.setEnabled(!isLoading);
                binding.leaveTeamButton.setEnabled(!isLoading);
                // if (binding.profileProgressBar != null) binding.profileProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getToastMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty() && getContext() != null) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                viewModel.clearToastMessage();
            }
        });

        viewModel.playerProfile.observe(getViewLifecycleOwner(), profile -> {
            if (binding == null) return;

            if (profile != null) {
                FirebaseUser currentUser = auth.getCurrentUser();
                String username = "N/A";
                if (profile.username != null && !profile.username.isEmpty()) {
                    username = profile.username;
                } else if (currentUser != null && currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
                    username = currentUser.getDisplayName();
                }

                Log.d(TAG, "Player Username: " + username);
                // No specific TextView for username in the confirmed XML, so just logging.
                // The main dialog title (@string/profile_title) has no ID.

                if (binding.statsPoints != null) {
                    binding.statsPoints.setText(String.valueOf(profile.individualXP));
                } else {
                    Log.e(TAG, "statsPoints TextView not found in layout!");
                }

                Log.d("ProfileFragment", "Player Stamina (from Profile): " + profile.stamina);
                Log.d("ProfileFragment", "Player Coins (from Profile): " + profile.coins);

                // Other existing stats like Distance, Playtime, Rank are not directly from PlayerProfile model yet.
                // They would need their own LiveData or be part of PlayerProfile if they are to be dynamic.

                boolean isInTeam = profile.teamId != null && !profile.teamId.isEmpty();
                binding.leaveTeamButton.setVisibility(isInTeam ? View.VISIBLE : View.GONE);
                binding.createTeamLayout.setVisibility(isInTeam ? View.GONE : View.VISIBLE);
                binding.joinTeamLayout.setVisibility(isInTeam ? View.GONE : View.VISIBLE);
            } else {
                 // UI updates for when profile is null
                 binding.currentTeamNameTextView.setText("Team: Not in a team");
                 binding.currentTeamIdTextView.setText("");
                 binding.leaveTeamButton.setVisibility(View.GONE);
                 binding.createTeamLayout.setVisibility(View.VISIBLE);
                 binding.joinTeamLayout.setVisibility(View.VISIBLE);
                 if (binding.statsPoints != null) binding.statsPoints.setText("0");
                 Log.d("ProfileFragment", "PlayerProfile is null. Displaying default dashboard state.");
            }
        });

        viewModel.currentTeam.observe(getViewLifecycleOwner(), team -> {
            if (binding == null) return;

            if (team != null) {
                binding.currentTeamNameTextView.setText("Team: " + team.teamName);
                binding.currentTeamIdTextView.setText("ID: " + team.teamId);
            } else {
                PlayerProfile profile = viewModel.playerProfile.getValue();
                if (profile == null || profile.teamId == null || profile.teamId.isEmpty()) {
                     binding.currentTeamNameTextView.setText("Team: Not in a team");
                     binding.currentTeamIdTextView.setText("");
                } else {
                    binding.currentTeamNameTextView.setText("Team: (Loading...)"); // Was "Error loading data"
                    binding.currentTeamIdTextView.setText("ID: " + profile.teamId);
                     Log.w(TAG, "Current team is null, but profile has teamId: " + profile.teamId);
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        Log.d(TAG, "onDestroyView called, binding set to null.");
    }
}
