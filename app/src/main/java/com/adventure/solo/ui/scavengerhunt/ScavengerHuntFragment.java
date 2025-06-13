package com.adventure.solo.ui.scavengerhunt;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.adventure.solo.R;
import com.adventure.solo.databinding.FragmentScavengerHuntBinding;
import com.adventure.solo.model.Clue; // Still needed for Clue specific fields
import com.adventure.solo.model.PlayerProfile; // For observing
import com.adventure.solo.model.Quest; // Still needed for Quest specific fields
import com.adventure.solo.model.wrapper.ClueWithProgress;
import com.adventure.solo.model.wrapper.QuestWithProgress;
import com.adventure.solo.model.ClueType; // Added for puzzle check
import com.adventure.solo.ui.ar.ARObjectInteractionListener;
import com.adventure.solo.ui.ar.ARSceneFragment;
import com.adventure.solo.ui.puzzle.PuzzleDisplayFragment; // Added for puzzle dialog

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;
// Removed @Inject for QuestManager as it's not directly used by Fragment anymore
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ScavengerHuntFragment extends Fragment implements ARObjectInteractionListener, PuzzleDisplayFragment.PuzzleSolvedListener { // Added PuzzleSolvedListener
    private static final String TAG = "ScavengerHuntFrag";

    private FragmentScavengerHuntBinding binding;
    private ScavengerHuntViewModel viewModel;

    // currentQuestForFragmentContext is now QuestWithProgress
    private QuestWithProgress currentQuestForFragmentContext;

    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay;
    private CompassOverlay compassOverlay;
    private RotationGestureOverlay rotationGestureOverlay;
    private List<Marker> clueMarkers = new ArrayList<>();
    // private Location lastKnownLocation; // Replaced by direct use of myLocationOverlay.getLastFix()

    private static final float PROXIMITY_RADIUS_FOR_AR_METERS = 30.0f;


    public ScavengerHuntFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentScavengerHuntBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(ScavengerHuntViewModel.class);

        this.mapView = binding.map;
        // Map init moved to onViewCreated to ensure view is fully available

        binding.searchButton.setOnClickListener(v -> viewModel.performSearch());

        // Example: Add a button to manually trigger loading the active quest for the team.
        // If you add a button with R.id.load_quest_button to your layout:
        // binding.loadQuestButton.setOnClickListener(v -> viewModel.loadActiveQuestForTeam());

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeMap(); // Initialize map after view is created
        observeViewModel();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called.");
        if (mapView != null) mapView.onResume();
        if (myLocationOverlay != null) myLocationOverlay.enableMyLocation();
        if (compassOverlay != null) compassOverlay.enableCompass();

        // Refresh user profile and then attempt to load active quest if necessary
        viewModel.refreshUserProfile();
        // Subsequent logic to load quest will be triggered by playerProfile LiveData observer.
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called.");
        if (mapView != null) mapView.onPause();
        if (myLocationOverlay != null) myLocationOverlay.disableMyLocation();
        if (compassOverlay != null) compassOverlay.disableCompass();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called.");
        if (mapView != null) mapView.onDetach();
        mapView = null;
        myLocationOverlay = null; compassOverlay = null; rotationGestureOverlay = null;
        clueMarkers.clear();
        binding = null;
    }

    private void initializeMap() {
        if (getContext() == null || mapView == null) {
            Log.e(TAG, "Cannot initialize map, context or mapView is null.");
            return;
        }
        Context ctx = requireActivity().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(16.0);

        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(ctx), mapView);
        myLocationOverlay.enableMyLocation();
        // myLocationOverlay.enableFollowLocation(); // Can be annoying, user can re-center
        // if (getContext() != null && ContextCompat.getDrawable(requireContext(), R.drawable.ic_user_location_marker) != null) {
        //    myLocationOverlay.setPersonIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_user_location_marker));
        // }
        mapView.getOverlays().add(myLocationOverlay);

        myLocationOverlay.runOnFirstFix(() -> {
            if (getActivity() != null && myLocationOverlay.getMyLocation() != null) {
                 getActivity().runOnUiThread(() -> {
                    Log.d(TAG, "Map: First fix. Centering map.");
                    mapView.getController().animateTo(myLocationOverlay.getMyLocation());
                 });
            }
        });

        compassOverlay = new CompassOverlay(ctx, mapView);
        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);

        rotationGestureOverlay = new RotationGestureOverlay(mapView);
        rotationGestureOverlay.setEnabled(true);
        mapView.getOverlays().add(rotationGestureOverlay);
        Log.d(TAG, "Map initialized.");
    }

    private void observeViewModel() {
        viewModel.currentPlayerProfile.observe(getViewLifecycleOwner(), profile -> {
            if (binding == null) return;
            if (profile != null) {
                Log.d(TAG, "Observed currentPlayerProfile update: " + profile.username + ", Team: " + profile.teamId);
                binding.pointsTextView.setText("XP: " + profile.individualXP);
                binding.staminaTextView.setText("Stamina: " + profile.stamina);
                binding.coinsTextView.setText("Coins: " + profile.coins);

                // Logic to load team quest if profile is updated (e.g., joined a team)
                if (profile.teamId != null && !profile.teamId.isEmpty()) {
                    QuestWithProgress currentQ = viewModel.activeQuestWithProgress.getValue();
                    // If no active quest or if current active quest's teamId doesn't match profile's teamId
                    if (currentQ == null || currentQ.progress == null || !profile.teamId.equals(currentQ.progress.teamId)) {
                        Log.d(TAG, "Player profile updated with a team ID, or active quest mismatch. Loading active quest for team: " + profile.teamId);
                        viewModel.loadActiveQuestForTeam();
                    }
                } else { // No teamId in profile
                    Log.d(TAG, "Player profile has no team ID. Clearing active quest.");
                    viewModel.setActiveQuestForTeam(null); // This will clear mission text and markers
                }
            } else {
                Log.d(TAG, "Observed currentPlayerProfile is null.");
                binding.pointsTextView.setText("XP: --");
                binding.staminaTextView.setText("Stamina: --");
                binding.coinsTextView.setText("Coins: --");
                binding.missionTextView.setText("Login and join a team to play.");
                updateClueMarkers(new ArrayList<>()); // Clear markers
            }
        });

        viewModel.currentMissionText.observe(getViewLifecycleOwner(), missionText -> {
            if (binding != null) binding.missionTextView.setText(missionText != null ? missionText : "Loading mission...");
        });

        viewModel.activeQuestWithProgress.observe(getViewLifecycleOwner(), qwp -> {
            currentQuestForFragmentContext = qwp; // Keep context for AR
            if (qwp == null || qwp.quest == null) {
                 Log.d(TAG, "Observed activeQuestWithProgress is null or quest is null.");
                 updateClueMarkers(new ArrayList<>());
            } else {
                Log.d(TAG, "Observed activeQuestWithProgress: " + qwp.quest.getTitle());
                // Clue markers are updated via currentQuestCluesWithProgress observer
            }
        });

        viewModel.currentQuestCluesWithProgress.observe(getViewLifecycleOwner(), this::updateClueMarkers);
    }

    @Override
    public void onARElementCollected(long clueId, int rewardPoints) {
        Log.d(TAG, "AR Element Collected by Player! Clue ID: " + clueId + ", Reward Points: " + rewardPoints);
        // ViewModel now handles interaction with QuestManager and PlayerProfileRepository
        viewModel.clueCollectedByPlayer(clueId, rewardPoints);
    }

    private void updateClueMarkers(List<ClueWithProgress> cluesWithProgressList) {
        if (mapView == null || getContext() == null) {
            Log.e(TAG, "updateClueMarkers: mapView or context is null.");
            return;
        }

        for (Marker oldMarker : clueMarkers) {
            mapView.getOverlays().remove(oldMarker);
        }
        clueMarkers.clear();

        if (cluesWithProgressList == null || cluesWithProgressList.isEmpty()) {
            mapView.invalidate();
            Log.d(TAG, "updateClueMarkers: No clues to display or list is null/empty.");
            return;
        }
        Log.d(TAG, "updateClueMarkers: Displaying " + cluesWithProgressList.size() + " clues.");

        for (ClueWithProgress cwp : cluesWithProgressList) {
            if (cwp.clue == null) {
                Log.w(TAG, "ClueWithProgress contains a null Clue object.");
                continue;
            }
            Clue clue = cwp.clue;
            boolean discovered = (cwp.progress != null && cwp.progress.discoveredByTeam);

            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(clue.getTargetLatitude(), clue.getTargetLongitude()));
            marker.setRelatedObject(cwp);
            marker.setTitle(clue.getText() != null ? clue.getText().substring(0, Math.min(clue.getText().length(), 20)) + "..." : "Clue");

            marker.setIcon(ContextCompat.getDrawable(requireContext(),
                discovered ? R.drawable.ic_marker_discovered : R.drawable.ic_marker_undiscovered));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

            marker.setOnMarkerClickListener((m, mv) -> {
                ClueWithProgress clickedCwp = (ClueWithProgress) m.getRelatedObject();
                if (clickedCwp != null) showClueDetailsDialog(clickedCwp);
                return true;
            });
            clueMarkers.add(marker);
            mapView.getOverlays().add(marker);
        }
        mapView.invalidate();
    }

    private void showClueDetailsDialog(ClueWithProgress cwp) {
        if (cwp == null || cwp.clue == null || getContext() == null) {
            Log.e(TAG, "showClueDetailsDialog: ClueWithProgress or Clue or Context is null.");
            return;
        }
        Clue clue = cwp.clue;
        boolean discoveredByTeam = (cwp.progress != null && cwp.progress.discoveredByTeam);

        // If it's an undiscovered puzzle clue, show the puzzle dialog
        if (!discoveredByTeam &&
            (clue.getClueType() == ClueType.PUZZLE_TEXT_RIDDLE || clue.getClueType() == ClueType.PUZZLE_MATH_SIMPLE)) {

            if (clue.getPuzzleData() == null || clue.getPuzzleData().isEmpty()) {
                Toast.makeText(getContext(), "Puzzle data missing for this clue.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Puzzle data is null or empty for clue ID: " + clue.getId());
                // Show basic info instead if puzzle data is bad
                showBasicClueInfoDialog(clue, discoveredByTeam);
                return;
            }

            PuzzleDisplayFragment puzzleDialog = PuzzleDisplayFragment.newInstance(
                clue.getId(),
                clue.getClueType(),
                clue.getPuzzleData()
            );
            puzzleDialog.setPuzzleSolvedListener(this); // ScavengerHuntFragment is the listener
            if (getParentFragmentManager() != null) {
                puzzleDialog.show(getParentFragmentManager(), PuzzleDisplayFragment.TAG);
            } else {
                Log.e(TAG, "getParentFragmentManager() is null, cannot show PuzzleDisplayFragment.");
            }
        } else {
            // Original dialog logic for location clues or already solved puzzles
            showBasicClueInfoDialog(clue, discoveredByTeam, cwp); // Pass CWP for AR launch
        }
    }

    private void showBasicClueInfoDialog(Clue clue, boolean discoveredByTeam, @Nullable ClueWithProgress cwpForAr) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Clue: " + (clue.getText() != null ? clue.getText().substring(0, Math.min(clue.getText().length(), 25))+"..." : "Details"));

        StringBuilder message = new StringBuilder(clue.getText() != null ? clue.getText() : "No text available.");
        message.append("\n\nStatus: ").append(discoveredByTeam ? "Discovered by Team" : "Not Yet Discovered");

        if (!discoveredByTeam && clue.getHint() != null && !clue.getHint().isEmpty()) {
            message.append("\n\nHint: ").append(clue.getHint());
        }
        builder.setMessage(message.toString());
        builder.setPositiveButton("OK", null);

        // Only show "View in AR" for LOCATION clues, if not discovered, and if player is near.
        if (clue.getClueType() == ClueType.LOCATION && !discoveredByTeam && cwpForAr != null && isPlayerNearClue(clue, PROXIMITY_RADIUS_FOR_AR_METERS)) {
            builder.setNeutralButton("View in AR", (dialog, which) -> launchArForClue(cwpForAr));
        }
        builder.create().show();
    }

    // Overload for when CWP is not needed (e.g. after puzzle data error)
    private void showBasicClueInfoDialog(Clue clue, boolean discoveredByTeam) {
        showBasicClueInfoDialog(clue, discoveredByTeam, null);
    }

    private boolean isPlayerNearClue(Clue clue, float proximityRadiusMeters) {
        Location deviceLocation = null;
        if (myLocationOverlay != null && myLocationOverlay.getLastFix() != null) {
            deviceLocation = myLocationOverlay.getLastFix();
        } else if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
            GeoPoint currentGeoPoint = myLocationOverlay.getMyLocation();
            deviceLocation = new Location("");
            deviceLocation.setLatitude(currentGeoPoint.getLatitude());
            deviceLocation.setLongitude(currentGeoPoint.getLongitude());
        }

        if (deviceLocation == null) {
            if(getContext() != null) Toast.makeText(getContext(), "Current location unavailable for proximity check.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "isPlayerNearClue: deviceLocation is null.");
            return false;
        }
        if (clue == null) {
            Log.e(TAG, "isPlayerNearClue: Clue object is null.");
            return false;
        }
        Location clueLocation = new Location("");
        clueLocation.setLatitude(clue.getTargetLatitude());
        clueLocation.setLongitude(clue.getTargetLongitude());
        float distance = deviceLocation.distanceTo(clueLocation);
        Log.d(TAG, "isPlayerNearClue: Distance to clue " + clue.getId() + " is " + distance + "m. Radius: " + proximityRadiusMeters + "m.");
        return distance < proximityRadiusMeters;
    }

    private void launchArForClue(ClueWithProgress cwp) {
        QuestWithProgress activeQwp = viewModel.activeQuestWithProgress.getValue();
        if (cwp == null || cwp.clue == null || activeQwp == null || activeQwp.quest == null || getContext() == null) {
             if(getContext() != null) Toast.makeText(getContext(), "Cannot launch AR: Critical data missing (clue/quest).", Toast.LENGTH_SHORT).show();
             Log.e(TAG, "launchArForClue: Prerequisite data missing.");
             return;
        }
        Clue clue = cwp.clue;
        Quest quest = activeQwp.quest;

        Log.d(TAG, "Launching AR for Clue ID: " + clue.getId() + " of Quest: " + quest.getTitle());
        ARSceneFragment arFragment = ARSceneFragment.newInstance(
            clue.getTargetLatitude(), clue.getTargetLongitude(), clue.getId(), quest.getRewardPoints());

        // Ensure R.id.nav_host_fragment (or nav_host_fragment_content_main) is correct for MainActivity's NavHost
        getParentFragmentManager().beginTransaction()
            .replace(R.id.nav_host_fragment_content_main, arFragment)
            .addToBackStack(TAG) // Use fragment's TAG for backstack name
            .commit();
    }
}
