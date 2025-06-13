package com.adventure.solo.ui.inventory;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.adventure.solo.databinding.FragmentInventoryBinding;
import java.util.ArrayList; // For initializing adapter with empty list
import dagger.hilt.android.AndroidEntryPoint;
import android.util.Log;

@AndroidEntryPoint
public class InventoryFragment extends Fragment {
    private static final String TAG = "InventoryFragment";

    private InventoryViewModel viewModel;
    private FragmentInventoryBinding binding;
    private CompletedQuestAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentInventoryBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(InventoryViewModel.class);

        setupRecyclerView();
        observeViewModel();

        Log.d(TAG, "onCreateView completed.");
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh user data which in turn might trigger reloading completed quests
        // if the user or their team status changed.
        Log.d(TAG, "onResume: Calling refreshUser in ViewModel.");
        viewModel.refreshUser();
    }

    private void setupRecyclerView() {
        adapter = new CompletedQuestAdapter();
        binding.completedQuestsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.completedQuestsRecyclerView.setAdapter(adapter);
        Log.d(TAG, "RecyclerView setup completed.");
    }

    private void observeViewModel() {
        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            // TODO: Show progress bar if one is added to fragment_inventory.xml
            // e.g., binding.inventoryProgressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            Log.d(TAG, "isLoading LiveData changed: " + isLoading);
        });

        viewModel.completedQuestsWithDetails.observe(getViewLifecycleOwner(), quests -> {
            Log.d(TAG, "completedQuestsWithDetails LiveData changed. Quest count: " + (quests != null ? quests.size() : "null"));
            if (quests != null && !quests.isEmpty()) {
                adapter.submitList(quests);
                binding.emptyInventoryTextView.setVisibility(View.GONE);
                binding.completedQuestsRecyclerView.setVisibility(View.VISIBLE);
            } else {
                adapter.submitList(new ArrayList<>()); // Submit empty list to clear adapter
                binding.emptyInventoryTextView.setVisibility(View.VISIBLE);
                binding.completedQuestsRecyclerView.setVisibility(View.GONE);
            }
        });

        // Observe player profile to potentially trigger initial load if teamId is present
        // This is now handled by the chained LiveData in ViewModel starting with _currentUserIdTrigger
        // viewModel.getPlayerProfile().observe(getViewLifecycleOwner(), profile -> {
        //    if (profile != null && profile.teamId != null && !profile.teamId.isEmpty()) {
        //        Log.d(TAG, "Player profile loaded with teamId: " + profile.teamId + ". ViewModel should be loading quests.");
        //    } else if (profile != null) {
        //        Log.d(TAG, "Player profile loaded but no teamId.");
        //        binding.emptyInventoryTextView.setText("Join a team to see completed team quests.");
        //    } else {
        //        Log.d(TAG, "Player profile is null.");
        //         binding.emptyInventoryTextView.setText("Login to see your inventory.");
        //    }
        // });

        Log.d(TAG, "ViewModel observers setup.");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.completedQuestsRecyclerView.setAdapter(null); // Clear adapter to help GC
        binding = null; // Crucial for ViewBinding in Fragments
        Log.d(TAG, "onDestroyView completed.");
    }
}
