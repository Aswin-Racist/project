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
import com.adventure.solo.model.Clue;
import com.adventure.solo.model.Quest;
import com.adventure.solo.service.QuestManager;
import com.adventure.solo.ui.ar.ARObjectInteractionListener;
import com.adventure.solo.ui.ar.ARSceneFragment;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ScavengerHuntFragment extends Fragment implements ARObjectInteractionListener {

    private static final String TAG = "ScavengerHuntFragment";
    private FragmentScavengerHuntBinding binding;
    private ScavengerHuntViewModel viewModel;

    @Inject
    QuestManager questManager;

    private Quest currentQuestForFragmentContext;

    // Map related fields
    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay;
    private CompassOverlay compassOverlay;
    private RotationGestureOverlay rotationGestureOverlay;
    private List<Marker> clueMarkers = new ArrayList<>();
    private static final float PROXIMITY_RADIUS_FOR_AR_METERS = 30.0f;

    public ScavengerHuntFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentScavengerHuntBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(ScavengerHuntViewModel.class);

        this.mapView = binding.map;

        binding.searchButton.setOnClickListener(v -> {
            if (!viewModel.performSearch()) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Not enough stamina to search!", Toast.LENGTH_SHORT).show();
                }
            } else {
                 if (getContext() != null) {
                    Toast.makeText(getContext(), "Searched! +5 Coins, -10 Stamina", Toast.LENGTH_SHORT).show();
                 }
            }
        });

        // Example: Recenter button (if you add one with this ID)
        // if (binding.recenterMapButton != null) {
        //    binding.recenterMapButton.setOnClickListener(v -> {
        //        if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
        //            mapView.getController().animateTo(myLocationOverlay.getMyLocation());
        //        } else if (myLocationOverlay != null && myLocationOverlay.getLastFix() != null) {
        //            GeoPoint lastFixGeoPoint = new GeoPoint(myLocationOverlay.getLastFix().getLatitude(), myLocationOverlay.getLastFix().getLongitude());
        //            mapView.getController().animateTo(lastFixGeoPoint);
        //        } else {
        //            Toast.makeText(getContext(), "Current location not available.", Toast.LENGTH_SHORT).show();
        //        }
        //    });
        // }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeMap();
        observeViewModel();
        // TODO: Load initial quest/mission state.
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
        mapView.getController().setZoom(16.0); // Slightly more zoom

        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(ctx), mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        // Make sure R.drawable.ic_user_location_marker exists
        // myLocationOverlay.setPersonIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_user_location_marker));

        myLocationOverlay.runOnFirstFix(() -> {
            if (getActivity() != null && myLocationOverlay.getMyLocation() != null) {
                 getActivity().runOnUiThread(() -> {
                     mapView.getController().animateTo(myLocationOverlay.getMyLocation());
                 });
            }
        });
        mapView.getOverlays().add(myLocationOverlay);

        compassOverlay = new CompassOverlay(ctx, mapView);
        compassOverlay.enableCompass();
        mapView.getOverlays().add(compassOverlay);

        rotationGestureOverlay = new RotationGestureOverlay(mapView);
        rotationGestureOverlay.setEnabled(true);
        mapView.getOverlays().add(rotationGestureOverlay);
        Log.d(TAG, "Map initialized.");
    }

    private void observeViewModel() {
        viewModel.getPlayerScore().observe(getViewLifecycleOwner(), score -> {
            if (score != null && binding != null && binding.pointsTextView != null) {
                binding.pointsTextView.setText("Points: " + score);
            }
        });
        viewModel.getCurrentMissionText().observe(getViewLifecycleOwner(), missionText -> {
            if (binding != null && binding.missionTextView != null) {
                binding.missionTextView.setText(missionText);
            }
        });
        viewModel.getActiveQuest().observe(getViewLifecycleOwner(), quest -> {
            currentQuestForFragmentContext = quest;
            if (quest != null) {
                // This was missing in the proposed diff, but is in the ViewModel's logic.
                // When activeQuest changes, ViewModel should load its clues.
                // This observer in Fragment is mainly for context.
                // The currentQuestClues observer will handle marker updates.
                Log.d(TAG, "Active quest changed in Fragment: " + quest.getTitle());
            } else {
                updateClueMarkers(new ArrayList<>()); // Clear markers if no active quest
                Log.d(TAG, "Active quest is null in Fragment.");
            }
        });
        viewModel.getCurrentQuestClues().observe(getViewLifecycleOwner(), this::updateClueMarkers);

        viewModel.getPlayerStamina().observe(getViewLifecycleOwner(), stamina -> {
            if (stamina != null && binding != null && binding.staminaTextView != null) {
                 binding.staminaTextView.setText("Stamina: " + stamina);
            }
        });
        viewModel.getPlayerCoins().observe(getViewLifecycleOwner(), coins -> {
            if (coins != null && binding != null && binding.coinsTextView != null) {
                binding.coinsTextView.setText("Coins: " + coins);
            }
        });
    }

    @Override
    public void onARElementCollected(long clueId, int rewardPoints) {
        Log.d(TAG, "AR Element Collected: Clue ID " + clueId + ", Points: " + rewardPoints);
        viewModel.addScore(rewardPoints);
        if (currentQuestForFragmentContext != null) {
             viewModel.clueCollected(clueId, currentQuestForFragmentContext);
        } else {
            Log.e(TAG, "currentQuestForFragmentContext is null in onARElementCollected");
            Toast.makeText(getContext(), "Error processing clue collection (no quest context).", Toast.LENGTH_LONG).show();
        }
    }

    private void updateClueMarkers(List<Clue> clues) {
        if (mapView == null || getContext() == null) return;

        List<org.osmdroid.views.overlay.Overlay> mapOverlays = mapView.getOverlays();
        // More careful removal of only clue markers
        for (Marker oldMarker : clueMarkers) {
            mapOverlays.remove(oldMarker);
        }
        clueMarkers.clear();

        if (clues == null || clues.isEmpty()) {
            mapView.invalidate();
            return;
        }

        for (Clue clue : clues) {
            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(clue.getTargetLatitude(), clue.getTargetLongitude()));
            marker.setRelatedObject(clue);
            marker.setTitle(clue.getText().substring(0, Math.min(clue.getText().length(), 20)) + "...");

            if (clue.isDiscovered()) {
                marker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_marker_discovered));
            } else {
                marker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_marker_undiscovered));
            }
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

            marker.setOnMarkerClickListener((m, mv) -> {
                Clue clickedClue = (Clue) m.getRelatedObject();
                if (clickedClue != null) {
                    showClueDetailsDialog(clickedClue);
                }
                return true;
            });
            clueMarkers.add(marker);
            mapOverlays.add(marker);
        }
        mapView.invalidate();
    }

    private void showClueDetailsDialog(Clue clue) {
        if (getContext() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext()); // Use getContext()
        builder.setTitle("Clue: " + clue.getText().substring(0, Math.min(clue.getText().length(), 25))+"...");

        StringBuilder message = new StringBuilder();
        message.append(clue.getText()).append("\n\nStatus: ")
               .append(clue.isDiscovered() ? "Discovered" : "Not Discovered");

        if (!clue.isDiscovered() && clue.getHint() != null && !clue.getHint().isEmpty()) {
            message.append("\n\nHint: ").append(clue.getHint());
        }
        builder.setMessage(message.toString());
        builder.setPositiveButton("OK", null);

        if (!clue.isDiscovered() && isPlayerNearClue(clue, PROXIMITY_RADIUS_FOR_AR_METERS)) {
            builder.setNeutralButton("View in AR", (dialog, which) -> {
                launchArForClue(clue);
            });
        }
        builder.create().show();
    }

    private boolean isPlayerNearClue(Clue clue, float proximityRadiusMeters) {
        Location deviceLocation = null;
        if (myLocationOverlay != null && myLocationOverlay.getLastFix() != null) {
            deviceLocation = myLocationOverlay.getLastFix();
        } else if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
            GeoPoint currentGeoPoint = myLocationOverlay.getMyLocation();
            deviceLocation = new Location(""); // Provider name is optional
            deviceLocation.setLatitude(currentGeoPoint.getLatitude());
            deviceLocation.setLongitude(currentGeoPoint.getLongitude());
        }

        if (deviceLocation == null) {
            if (getContext() != null) Toast.makeText(getContext(), "Current location not available.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "Cannot check proximity for AR: deviceLocation is null.");
            return false;
        }

        Location clueLocation = new Location("");
        clueLocation.setLatitude(clue.getTargetLatitude());
        clueLocation.setLongitude(clue.getTargetLongitude());
        float distance = deviceLocation.distanceTo(clueLocation);
        Log.d(TAG, "Distance to clue " + clue.getId() + " for AR check: " + distance + "m");
        return distance < proximityRadiusMeters;
    }

    private void launchArForClue(Clue clue) {
        if (currentQuestForFragmentContext == null) {
             if (getContext() != null) Toast.makeText(getContext(), "No active quest to launch AR for.", Toast.LENGTH_SHORT).show();
             return;
        }
        ARSceneFragment arFragment = ARSceneFragment.newInstance(
            clue.getTargetLatitude(),
            clue.getTargetLongitude(),
            clue.getId(),
            currentQuestForFragmentContext.getRewardPoints()
        );
        getParentFragmentManager().beginTransaction()
            .replace(R.id.nav_host_fragment_content_main, arFragment)
            .addToBackStack(TAG) // Use TAG for backstack name for clarity
            .commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        if (myLocationOverlay != null) myLocationOverlay.enableMyLocation();
        if (compassOverlay != null) compassOverlay.enableCompass();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
        if (myLocationOverlay != null) myLocationOverlay.disableMyLocation();
        if (compassOverlay != null) compassOverlay.disableCompass();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mapView != null) {
            mapView.onDetach();
        }
        mapView = null;
        myLocationOverlay = null;
        compassOverlay = null;
        rotationGestureOverlay = null;
        clueMarkers.clear();
        binding = null;
    }
}
