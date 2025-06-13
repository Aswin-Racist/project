package com.adventure.solo.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.adventure.solo.databinding.ActivityAdminDashboardBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList; // For initializing empty list if needed

import dagger.hilt.android.AndroidEntryPoint;
import android.util.Log;

@AndroidEntryPoint
public class AdminDashboardActivity extends AppCompatActivity {
    private static final String TAG = "AdminDashboardActivity";

    private ActivityAdminDashboardBinding binding;
    private AdminViewModel viewModel;
    private TeamAdminAdapter adapter;

    // IMPORTANT: Replace with actual admin UIDs from your Firebase project's Authentication tab
    private static final List<String> ADMIN_UIDS = Arrays.asList(
        // "YOUR_ADMIN_FIREBASE_UID_1",
        // "YOUR_ADMIN_FIREBASE_UID_2"
        // Add your Firebase Admin UIDs here. For testing, you can use your own UID after logging in.
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        // Check if ADMIN_UIDS is empty for testing purposes if you haven't set your UID yet.
        // In production, this list should be properly populated.
        if (ADMIN_UIDS.isEmpty()) {
            Log.w(TAG, "ADMIN_UIDS list is empty. Access control will not function correctly.");
            // You might want to allow access for testing if the list is empty.
            // For this example, if list is empty, deny access for safety.
        }

        if (currentUser == null || !ADMIN_UIDS.contains(currentUser.getUid())) {
            Log.w(TAG, "Access Denied. UID: " + (currentUser != null ? currentUser.getUid() : "null") + ". Not in ADMIN_UIDS list.");
            binding.adminControlsLayout.setVisibility(View.GONE);
            binding.adminAccessDeniedTextView.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Access Denied. You are not an administrator.", Toast.LENGTH_LONG).show();
            // Consider finishing the activity: finish();
            return;
        }

        Log.i(TAG, "Admin access GRANTED for UID: " + currentUser.getUid());
        binding.adminControlsLayout.setVisibility(View.VISIBLE);
        binding.adminAccessDeniedTextView.setVisibility(View.GONE);

        viewModel = new ViewModelProvider(this).get(AdminViewModel.class);

        setupRecyclerView();
        setupClickListeners();
        observeViewModel();

        // Initial fetch if not handled by ViewModel's init
        // viewModel.fetchAllTeamsAndMembers();
    }

    private void setupRecyclerView() {
        adapter = new TeamAdminAdapter();
        binding.teamsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.teamsRecyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        binding.assignPlayerButton.setOnClickListener(v -> {
            String playerUid = binding.assignPlayerUidEditText.getText().toString().trim();
            String teamId = binding.assignTeamIdEditText.getText().toString().trim();
            // For assigning, playerUsername is not strictly required by TeamRepository's addPlayerToTeam
            // if the Team POJO doesn't store display names directly, or if it's fetched later.
            // Passing null or a placeholder if not available from UI.
            viewModel.assignPlayerToTeam(playerUid, teamId, "AssignedPlayer");
        });

        binding.disbandTeamButton.setOnClickListener(v -> {
            String teamId = binding.disbandTeamIdEditText.getText().toString().trim();
            viewModel.disbandTeam(teamId);
        });
    }

    private void observeViewModel() {
        // allTeams LiveData is mainly to trigger fetchMembersForTeams in ViewModel.
        // The adapter is updated by teamMembersMap observer.
        viewModel.allTeams.observe(this, teams -> {
            Log.d(TAG, "Observed allTeams update. Count: " + (teams != null ? teams.size() : "null"));
            // The actual update to adapter happens when teamMembersMap is updated.
        });

        viewModel.teamMembersMap.observe(this, teamMembersMap -> {
            Log.d(TAG, "Observed teamMembersMap update. Map size: " + (teamMembersMap != null ? teamMembersMap.size() : "null"));
            if (viewModel.allTeams.getValue() != null && teamMembersMap != null) {
                adapter.submitData(viewModel.allTeams.getValue(), teamMembersMap);
            } else {
                 adapter.submitData(new ArrayList<>(), new HashMap<>()); // Clear adapter if data is incomplete
                 Log.d(TAG, "Either allTeams or teamMembersMap is null, clearing adapter.");
            }
        });

        viewModel.isLoading.observe(this, isLoading -> {
            if (isLoading != null && isLoading) {
                // TODO: Show a more prominent loading indicator, e.g., a ProgressBar
                binding.assignPlayerButton.setEnabled(false);
                binding.disbandTeamButton.setEnabled(false);
            } else {
                binding.assignPlayerButton.setEnabled(true);
                binding.disbandTeamButton.setEnabled(true);
            }
        });

        viewModel.toastMessage.observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                viewModel.clearToastMessage();
            }
        });
    }
}
