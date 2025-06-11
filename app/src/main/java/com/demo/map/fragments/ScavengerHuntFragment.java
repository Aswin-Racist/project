package com.demo.map.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.demo.map.R;
import com.demo.map.ScavengerHuntApplication;
import com.demo.map.game.ScavengerHuntGame;
import com.demo.map.model.GameReward;
import com.demo.map.model.Mission;
import com.demo.map.model.Player;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.demo.map.dialogs.DashboardDialog;
import com.demo.map.dialogs.ProfileDialog;

public class ScavengerHuntFragment extends Fragment {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final double DEFAULT_ZOOM = 17.0;
    private static final long LOCATION_UPDATE_INTERVAL = 5000;
    private static final float REWARD_COLLECTION_DISTANCE = 10f; // meters
    private static final float MAX_PLAY_RADIUS = 50f; // meters
    private static final float SCAN_RADIUS = 50f; // meters (changed from 200m)

    private MapView map;
    private IMapController mapController;
    private MyLocationNewOverlay locationOverlay;
    private ScavengerHuntGame game;
    private Player currentPlayer;
    private TextView pointsTextView;
    private TextView missionTextView;
    private TextView outOfRangeWarning;
    private Map<Marker, GameReward> rewardMarkers;
    private Map<String, Marker> playerMarkers;
    private GeoPoint lastCenterLocation;
    private FloatingActionButton profileButton;
    private FloatingActionButton dashboardButton;
    private FloatingActionButton recenterButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScavengerHuntApplication app = (ScavengerHuntApplication) requireActivity().getApplication();
        game = app.getGame();
        currentPlayer = app.getCurrentPlayer();
        rewardMarkers = new HashMap<>();
        playerMarkers = new HashMap<>();

        // Initialize OSMDroid configuration
        Context ctx = requireActivity().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scavenger_hunt, container, false);
        
        pointsTextView = view.findViewById(R.id.points_text_view);
        missionTextView = view.findViewById(R.id.mission_text_view);
        outOfRangeWarning = view.findViewById(R.id.outOfRangeWarning);
        
        profileButton = view.findViewById(R.id.profileButton);
        dashboardButton = view.findViewById(R.id.dashboardButton);
        recenterButton = view.findViewById(R.id.recenterButton);
        
        setupButtons(view);
        
        // Initialize map
        map = view.findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        
        mapController = map.getController();
        mapController.setZoom(DEFAULT_ZOOM);

        setupLocationOverlay();
        
        if (checkLocationPermission()) {
            startLocationUpdates();
        } else {
            requestLocationPermission();
        }
        
        return view;
    }

    private void setupButtons(View view) {
        profileButton.setOnClickListener(v -> showProfile());
        dashboardButton.setOnClickListener(v -> showDashboard());
        recenterButton.setOnClickListener(v -> recenterMap());
    }

    private void showProfile() {
        if (game != null) {
            ProfileDialog dialog = ProfileDialog.newInstance(game.getCurrentPlayer());
            dialog.show(getChildFragmentManager(), "profile");
        }
    }

    private void showDashboard() {
        DashboardDialog dialog = new DashboardDialog();
        dialog.show(getChildFragmentManager(), "dashboard");
    }

    private void setupLocationOverlay() {
        locationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), map);
        locationOverlay.enableMyLocation();
        locationOverlay.enableFollowLocation();
        locationOverlay.runOnFirstFix(() -> {
            requireActivity().runOnUiThread(() -> {
                GeoPoint location = locationOverlay.getMyLocation();
                if (location != null) {
                    lastCenterLocation = location;
                    mapController.animateTo(location);
                    updateNearbyRewards(locationToAndroid(location));
                    generateMissionsIfNeeded(locationToAndroid(location));
                    updateNearbyPlayers(location);
                }
            });
        });
        map.getOverlays().add(locationOverlay);
    }

    private void updateNearbyPlayers(GeoPoint currentLocation) {
        // Remove old player markers
        for (Marker marker : playerMarkers.values()) {
            map.getOverlays().remove(marker);
        }
        playerMarkers.clear();

        // Add markers for all players except current player
        List<Player> nearbyPlayers = game.getPlayers();
        for (Player player : nearbyPlayers) {
            if (!player.getId().equals(currentPlayer.getId()) && player.getLastLocation() != null) {
                GeoPoint playerLocation = new GeoPoint(
                    player.getLastLocation().getLatitude(),
                    player.getLastLocation().getLongitude()
                );
                
                // Only show players within the play radius
                float[] results = new float[1];
                android.location.Location.distanceBetween(
                    currentLocation.getLatitude(), currentLocation.getLongitude(),
                    playerLocation.getLatitude(), playerLocation.getLongitude(),
                    results
                );
                
                if (results[0] <= MAX_PLAY_RADIUS) {
                    Marker playerMarker = new Marker(map);
                    playerMarker.setPosition(playerLocation);
                    playerMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    playerMarker.setTitle(player.getName());
                    playerMarker.setSnippet(getString(R.string.points_format, player.getTotalPoints()));
                    
                    // Set player's avatar as marker icon
                    if (player.getAvatarResource() != null) {
                        try {
                            playerMarker.setIcon(getResources().getDrawable(
                                getResources().getIdentifier(
                                    player.getAvatarResource(),
                                    "drawable",
                                    requireContext().getPackageName()
                                )
                            ));
                        } catch (Exception e) {
                            playerMarker.setIcon(getResources().getDrawable(R.drawable.ic_person));
                        }
                    }
                    
                    map.getOverlays().add(playerMarker);
                    playerMarkers.put(player.getId(), playerMarker);
                }
            }
        }
        
        map.invalidate();

        // Update dashboard if it's open
        DashboardDialog dashboardDialog = (DashboardDialog) getChildFragmentManager()
            .findFragmentByTag("dashboard");
        if (dashboardDialog != null && dashboardDialog.isVisible()) {
            dashboardDialog.updateLeaderboard(game.getLeaderboard());
        }
    }

    private void checkOutOfRange(Location currentLocation) {
        if (lastCenterLocation != null) {
            float distance = currentLocation.distanceTo(locationToAndroid(lastCenterLocation));
            boolean isOutOfRange = distance > MAX_PLAY_RADIUS;
            outOfRangeWarning.setVisibility(isOutOfRange ? View.VISIBLE : View.GONE);
            
            if (isOutOfRange) {
                // Clear rewards that are too far
                clearDistantRewards(currentLocation);
            }
        }
    }

    private void clearDistantRewards(Location currentLocation) {
        List<GameReward> nearbyRewards = game.getNearbyRewards(currentLocation, MAX_PLAY_RADIUS);
        for (Map.Entry<Marker, GameReward> entry : new HashMap<>(rewardMarkers).entrySet()) {
            if (!nearbyRewards.contains(entry.getValue())) {
                map.getOverlays().remove(entry.getKey());
                rewardMarkers.remove(entry.getKey());
            }
        }
        map.invalidate();
    }

    private Location locationToAndroid(GeoPoint geoPoint) {
        Location location = new Location("");
        location.setLatitude(geoPoint.getLatitude());
        location.setLongitude(geoPoint.getLongitude());
        return location;
    }

    private void updateNearbyRewards(Location location) {
        // Clear old markers
        for (Marker marker : rewardMarkers.keySet()) {
            map.getOverlays().remove(marker);
        }
        rewardMarkers.clear();

        // Add new markers for nearby rewards
        List<GameReward> nearbyRewards = game.getNearbyRewards(location, 50);
        for (GameReward reward : nearbyRewards) {
            Marker marker = new Marker(map);
            marker.setPosition(new GeoPoint(reward.getLatitude(), reward.getLongitude()));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            
            switch (reward.getType()) {
                case COIN:
                    marker.setIcon(getResources().getDrawable(R.drawable.ic_coin));
                    break;
                case GOLD_BAR:
                    marker.setIcon(getResources().getDrawable(R.drawable.ic_gold_bar));
                    break;
                case TREASURE_CHEST:
                    marker.setIcon(getResources().getDrawable(R.drawable.ic_treasure));
                    break;
            }
            
            marker.setTitle(reward.getType().name());
            marker.setOnMarkerClickListener((m, mapView) -> {
                checkRewardCollection(reward, m);
                return true;
            });
            
            map.getOverlays().add(marker);
            rewardMarkers.put(marker, reward);
        }
        
        map.invalidate();
        updateUI();
    }

    private void generateMissionsIfNeeded(Location location) {
        if (currentPlayer.getActiveMissions().size() < 3) {
            Mission mission = game.generateMission(location);
            if (mission != null) {
                currentPlayer.addMission(mission);
                updateUI();
            }
        }
    }

    private void checkRewardCollection(GameReward reward, Marker marker) {
        GeoPoint playerLocation = locationOverlay.getMyLocation();
        if (playerLocation == null) {
            Toast.makeText(requireContext(), 
                getString(R.string.unable_to_determine_location), 
                Toast.LENGTH_SHORT).show();
            return;
        }

        Location rewardLocation = new Location("");
        rewardLocation.setLatitude(reward.getLatitude());
        rewardLocation.setLongitude(reward.getLongitude());

        Location playerLoc = locationToAndroid(playerLocation);
        float distance = playerLoc.distanceTo(rewardLocation);
        
        if (distance <= REWARD_COLLECTION_DISTANCE) {
            game.collectReward(currentPlayer, reward);
            map.getOverlays().remove(marker);
            rewardMarkers.remove(marker);
            map.invalidate();
            Toast.makeText(requireContext(), 
                String.format(getString(R.string.reward_collected), 
                    reward.getPoints()), 
                Toast.LENGTH_SHORT).show();
            updateUI();
        } else {
            Toast.makeText(requireContext(), 
                getString(R.string.get_closer), 
                Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUI() {
        pointsTextView.setText(String.format(getString(R.string.points_format), 
            currentPlayer.getTotalPoints()));
        
        StringBuilder missionsText = new StringBuilder("Active Missions:\n");
        for (Mission mission : currentPlayer.getActiveMissions()) {
            missionsText.append("- ").append(mission.getTitle())
                    .append("\n  ").append(mission.getClue())
                    .append("\n");
        }
        missionTextView.setText(missionsText.toString());
    }

    private void startLocationUpdates() {
        if (locationOverlay != null) {
            locationOverlay.enableMyLocation();
            locationOverlay.enableFollowLocation();
        }
    }

    private boolean checkLocationPermission() {
        return ActivityCompat.checkSelfPermission(requireContext(), 
            Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        requestPermissions(
            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
            LOCATION_PERMISSION_REQUEST_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(requireContext(), 
                    "Location permission is required for this game", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
        if (locationOverlay != null) {
            locationOverlay.enableMyLocation();
            locationOverlay.enableFollowLocation();
        }
        // Update nearby players when resuming
        if (lastCenterLocation != null) {
            updateNearbyPlayers(lastCenterLocation);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
        if (locationOverlay != null) {
            locationOverlay.disableMyLocation();
            locationOverlay.disableFollowLocation();
        }
    }

    private void recenterMap() {
        if (map != null && locationOverlay != null && locationOverlay.getMyLocation() != null) {
            GeoPoint myLocation = locationOverlay.getMyLocation();
            map.getController().animateTo(myLocation);
            map.getController().setZoom(17.0);
            lastCenterLocation = myLocation;
        }
    }
} 