package com.demo.map;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.demo.map.dialogs.AvatarSelectionDialog;

public class MapActivity extends AppCompatActivity implements AvatarSelectionDialog.OnAvatarSelectedListener {

    private static final String TAG = "MapActivity";
    private MapView mapView;
    private IMapController mapController;
    private MyLocationNewOverlay myLocationOverlay;
    private ProgressBar progressBar;
    private Button btnSearch;
    private ImageButton btnAvatar;
    private Marker userMarker;
    private List<Marker> huntMarkers = new ArrayList<>();
    private static final int HUNT_MARKER_COUNT = 5;
    private static final double HUNT_MARKER_MIN_DISTANCE = 5; // 5 meters
    private static final double HUNT_MARKER_MAX_DISTANCE = 10; // 10 meters
    private static final float MAP_ZOOM_LEVEL = 18.5f; // Higher zoom level for better visibility
    private String currentAvatar;
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            // Initialize osmdroid configuration
            Context ctx = getApplicationContext();
            Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
            
            // Set explicit user agent to avoid getting banned
            Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_NAME);
            
            // Enable hardware acceleration
            Configuration.getInstance().setMapViewHardwareAccelerated(true);
            
            // Set tile download threads for better performance
            Configuration.getInstance().setTileDownloadThreads((short) 4);
            Configuration.getInstance().setTileFileSystemThreads((short) 4);
            
            // Set cache size
            Configuration.getInstance().setTileFileSystemMaxQueueSize((short) 1024);
            Configuration.getInstance().setTileDownloadMaxQueueSize((short) 1024);
            
            // Set the layout
            setContentView(R.layout.activity_map);
            
            // Initialize views
            mapView = findViewById(R.id.mapView);
            progressBar = findViewById(R.id.progressBar);
            btnSearch = findViewById(R.id.btnSearch);
            btnAvatar = findViewById(R.id.btnAvatar);
            
            if (mapView != null) {
                // Configure map view
                mapView.setTileSource(TileSourceFactory.MAPNIK);
                mapView.setTilesScaledToDpi(true);
                mapView.setUseDataConnection(true);
                mapView.setBuiltInZoomControls(false);
                mapView.setMultiTouchControls(true);
                mapView.setMinZoomLevel(3.0);
                mapView.setMaxZoomLevel(21.0);
                mapView.setFlingEnabled(true);
                mapView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                
                // Set scrollable area limits
                mapView.setScrollableAreaLimitLatitude(
                    MapView.getTileSystem().getMaxLatitude(),
                    MapView.getTileSystem().getMinLatitude(),
                    0);
                mapView.setScrollableAreaLimitLongitude(
                    MapView.getTileSystem().getMinLongitude(),
                    MapView.getTileSystem().getMaxLongitude(),
                    0);
                
                // Initialize map controller
                mapController = mapView.getController();
                mapController.setZoom(15.0);
                
                // Request necessary permissions
                requestPermissionsIfNecessary(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.INTERNET
                });
                
                // Initialize location overlay
                initializeLocationOverlay();
                
                // Set default location (will be overridden by actual location)
                GeoPoint defaultPoint = new GeoPoint(27.7172, 85.3240);
                mapController.setCenter(defaultPoint);
                
                // Setup search button
                setupSearchButton();
                
                // Setup avatar selection
                setupAvatarSelection();
                
                // Set default avatar if none selected
                if (currentAvatar == null) {
                    currentAvatar = "whiteboy.png";
                    Prefs.saveAvatar(this, currentAvatar);
                }
            } else {
                Toast.makeText(this, "Error initializing map view", Toast.LENGTH_LONG).show();
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing map: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing map: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeLocationOverlay() {
        if (ContextCompat.checkSelfPermission(this, 
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
            myLocationOverlay.enableMyLocation();
            myLocationOverlay.enableFollowLocation();
            mapView.getOverlays().add(myLocationOverlay);
            
            // Center on location when available
            myLocationOverlay.runOnFirstFix(() -> runOnUiThread(() -> {
                GeoPoint myLocation = myLocationOverlay.getMyLocation();
                if (myLocation != null) {
                    mapController.animateTo(myLocation);
                    mapController.setZoom(16.0);
                    mapView.postDelayed(() -> {
                        BoundingBox boundingBox = mapView.getBoundingBox();
                        fetchPOIs(boundingBox);
                    }, 1000);
                }
            }));
        }
    }

    private void setupSearchButton() {
        btnSearch.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, 
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                scanArea(200);
            } else {
                requestPermissionsIfNecessary(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }
        });
    }

    private void fetchPOIs(BoundingBox boundingBox) {
        // Example Overpass API query for amenities in the current view
        String overpassQuery = "[out:json][timeout:25];(" +
                "node[\"amenity\"](" + boundingBox.getLatSouth() + "," + boundingBox.getLonWest() + 
                "," + boundingBox.getLatNorth() + "," + boundingBox.getLonEast() + ");" +
                ");out body;>;out skel qt;";

        String overpassUrl = "https://overpass-api.de/api/interpreter?data=" + overpassQuery;
        Log.d(TAG, "Overpass URL: " + overpassUrl);
        new FetchPOIsTask().execute(overpassUrl);
    }

    private class FetchPOIsTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            // Clear existing markers except MyLocationOverlay
            List<org.osmdroid.views.overlay.Overlay> overlaysToRemove = new ArrayList<>();
            for (org.osmdroid.views.overlay.Overlay overlay : mapView.getOverlays()) {
                if (!(overlay instanceof MyLocationNewOverlay)) {
                    overlaysToRemove.add(overlay);
                }
            }
            mapView.getOverlays().removeAll(overlaysToRemove);
            mapView.invalidate(); // Refresh the map
        }

        @Override
        protected String doInBackground(String... urls) {
            if (urls.length < 1 || urls[0] == null) {
                return null;
            }
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    return stringBuilder.toString();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching POIs: " + e.getMessage(), e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);
            progressBar.setVisibility(View.GONE);
            if (response != null) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONArray elements = jsonResponse.getJSONArray("elements");
                    for (int i = 0; i < elements.length(); i++) {
                        JSONObject element = elements.getJSONObject(i);
                        if (element.has("lat") && element.has("lon")) {
                            double lat = element.getDouble("lat");
                            double lon = element.getDouble("lon");
                            String name = element.optJSONObject("tags") != null ? 
                                element.optJSONObject("tags").optString("name", "N/A") : "N/A";
                            String amenityType = element.optJSONObject("tags") != null ? 
                                element.optJSONObject("tags").optString("amenity", "Unknown") : "Unknown";

                            HuntMarker poiMarker = new HuntMarker(mapView, new GeoPoint(lat, lon), 
                                name, "Type: " + amenityType, amenityType, name);
                            mapView.getOverlays().add(poiMarker);
                        }
                    }
                    mapView.invalidate(); // Refresh the map to show new markers
                    Toast.makeText(MapActivity.this, "Found " + elements.length() + " POIs", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing POIs: " + e.getMessage(), e);
                    Toast.makeText(MapActivity.this, "Error parsing POIs", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MapActivity.this, "Failed to fetch POIs", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        
        // Center on current location when resuming
        if (ContextCompat.checkSelfPermission(this, 
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            centerOnCurrentLocation();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permissions[i]);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        } else {
            // All permissions granted
            if (myLocationOverlay != null) {
                myLocationOverlay.enableMyLocation();
            }
        }
    }
    
    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void setupAvatarSelection() {
        // Initialize btnAvatar if not already done
        if (btnAvatar == null) {
            btnAvatar = findViewById(R.id.btnAvatar);
            if (btnAvatar == null) {
                Log.e(TAG, "Avatar button not found in layout");
                return;
            }
        }
        
        // Load saved avatar or use default
        currentAvatar = Prefs.getAvatar(this);
        if (currentAvatar == null || currentAvatar.isEmpty()) {
            currentAvatar = "whiteboy.png";
            Prefs.saveAvatar(this, currentAvatar);
        }
        
        Log.d(TAG, "Current avatar: " + currentAvatar);
        
        // Ensure the avatar file exists in assets
        try {
            String[] files = getAssets().list("drawable/avatars");
            if (files != null) {
                Log.d(TAG, "Available avatars: " + Arrays.toString(files));
            }
        } catch (IOException e) {
            Log.e(TAG, "Error listing avatars directory", e);
        }
        
        // Update avatar button
        updateAvatarButton();
        
        // Set click listener for avatar button
        btnAvatar.setOnClickListener(v -> {
            // Show avatar selection dialog
            AvatarSelectionDialog dialog = new AvatarSelectionDialog();
            dialog.show(getSupportFragmentManager(), "avatar_selection");
        });
        
        // Initial update of user marker
        updateUserMarker();
    }

    private void centerOnCurrentLocation() {
        if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
            GeoPoint myLocation = myLocationOverlay.getMyLocation();
            mapController.animateTo(myLocation);
            mapController.setZoom(MAP_ZOOM_LEVEL);
            updateUserMarker();
            
            // Generate hunt markers around the user
            generateHuntMarkers(myLocation);
            
            // Auto-scan the area around the user
            scanArea(200); // 200 meters radius
        } else {
            // Fallback to last known location
            Location lastKnownLocation = LocationUtils.getLastKnownLocation(this);
            if (lastKnownLocation != null) {
                GeoPoint startPoint = new GeoPoint(
                    lastKnownLocation.getLatitude(), 
                    lastKnownLocation.getLongitude()
                );
                mapController.animateTo(startPoint);
                mapController.setZoom(16.0);
                updateUserMarker();
            }
        }
    }
    
    private void scanArea(int radiusMeters) {
        if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
            GeoPoint center = myLocationOverlay.getMyLocation();
            
            // Calculate bounding box for the radius
            double lat = center.getLatitude();
            double lon = center.getLongitude();
            
            // Convert meters to degrees (approximate)
            double latDelta = (radiusMeters / 111320.0);
            double lonDelta = (radiusMeters / (111320.0 * Math.cos(Math.toRadians(lat))));
            
            BoundingBox boundingBox = new BoundingBox(
                lat + latDelta, 
                lon + lonDelta,
                lat - latDelta,
                lon - lonDelta
            );
            
            fetchPOIs(boundingBox);
        } else {
            Toast.makeText(this, "Location not available. Please enable location services.", 
                Toast.LENGTH_SHORT).show();
        }
    }

    private void updateAvatarButton() {
        if (btnAvatar == null) {
            Log.e(TAG, "Avatar button is null");
            return;
        }

        try {
            Log.d(TAG, "Loading avatar for button: " + currentAvatar);
            InputStream is = getAssets().open("drawable/avatars/" + currentAvatar);
            if (is == null) {
                Log.e(TAG, "Failed to open avatar file: " + currentAvatar);
                return;
            }
            
            Bitmap original = BitmapFactory.decodeStream(is);
            if (original == null) {
                Log.e(TAG, "Failed to decode avatar: " + currentAvatar);
                is.close();
                return;
            }
            
            // Scale down the bitmap for the button
            Bitmap scaled = Bitmap.createScaledBitmap(original, 200, 200, true);
            btnAvatar.setImageBitmap(scaled);
            
            // Clean up
            original.recycle();
            is.close();
            
            Log.d(TAG, "Avatar button updated with: " + currentAvatar);
            
        } catch (IOException e) {
            Log.e(TAG, "Error loading avatar for button: " + currentAvatar, e);
            // Fallback to a default icon
            btnAvatar.setImageResource(android.R.drawable.ic_menu_compass);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Out of memory error loading avatar", e);
            // Try with smaller bitmap
            try {
                Bitmap fallback = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
                fallback.eraseColor(Color.BLUE);
                btnAvatar.setImageBitmap(fallback);
            } catch (Exception ex) {
                Log.e(TAG, "Failed to create fallback bitmap", ex);
            }
        }
    }

    private void updateUserMarker() {
        runOnUiThread(() -> {
            // Add a debug marker at the center of the map
            Marker debugMarker = new Marker(mapView);
            GeoPoint centerPoint = new GeoPoint(mapView.getMapCenter().getLatitude(), mapView.getMapCenter().getLongitude());
            debugMarker.setPosition(centerPoint);
            debugMarker.setIcon(getResources().getDrawable(android.R.drawable.ic_dialog_alert));
            debugMarker.setTitle("Debug Marker");
            mapView.getOverlays().add(debugMarker);
            mapView.invalidate();
            
            Log.d(TAG, "Debug marker added at: " + centerPoint);
            if (myLocationOverlay == null || myLocationOverlay.getMyLocation() == null) {
                Log.e(TAG, "Cannot update user marker: location not available");
                return;
            }

            GeoPoint location = myLocationOverlay.getMyLocation();
            Log.d(TAG, "Updating user marker at location: " + location);

            // Remove existing user marker if any
            if (userMarker != null) {
                mapView.getOverlays().remove(userMarker);
            }
            
            // Add a temporary test marker to verify marker visibility
            Marker testMarker = new Marker(mapView);
            GeoPoint myLocation = new GeoPoint(myLocationOverlay.getMyLocation().getLatitude(), myLocationOverlay.getMyLocation().getLongitude());
            testMarker.setPosition(myLocation);
            testMarker.setIcon(getResources().getDrawable(android.R.drawable.ic_dialog_map));
            testMarker.setTitle("Test Marker");
            mapView.getOverlays().add(testMarker);
            Log.d(TAG, "Test marker added at: " + myLocation);

            try {
                // Log the avatar file we're trying to load
                Log.d(TAG, "Loading avatar: " + currentAvatar);
                
                // Try to load the avatar from assets
                InputStream is = getAssets().open("drawable/avatars/" + currentAvatar);
                if (is == null) {
                    throw new IOException("Failed to open avatar file: " + currentAvatar);
                }
                
                // Decode and scale the bitmap
                Bitmap original = BitmapFactory.decodeStream(is);
                if (original == null) {
                    throw new IOException("Failed to decode avatar: " + currentAvatar);
                }
                
                // Scale to appropriate size for map marker (smaller size for better performance)
                Bitmap scaled = Bitmap.createScaledBitmap(original, 150, 150, true);
                is.close();
                original.recycle();

                // Create a bordered drawable for better visibility
                Drawable iconDrawable = new BitmapDrawable(getResources(), scaled);
                
                // Create and add marker
                userMarker = new Marker(mapView);
                userMarker.setPosition(location);
                userMarker.setIcon(iconDrawable);
                userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                userMarker.setTitle("You are here");
                
                // Make sure the marker is above other overlays
                mapView.getOverlays().add(0, userMarker);
                
                // Force a redraw
                mapView.invalidate();
                Log.d(TAG, "User marker updated with avatar: " + currentAvatar);
                
            } catch (IOException e) {
                Log.e(TAG, "Error loading avatar: " + currentAvatar, e);
                
                // Fallback to default marker
                userMarker = new Marker(mapView);
                userMarker.setPosition(location);
                userMarker.setIcon(getResources().getDrawable(android.R.drawable.ic_menu_compass));
                userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                userMarker.setTitle("You are here (default)");
                mapView.getOverlays().add(0, userMarker);
                mapView.invalidate();
            }
        });
    }
    
    private void generateHuntMarkers(GeoPoint center) {
        // Clear existing hunt markers
        for (Marker marker : huntMarkers) {
            mapView.getOverlays().remove(marker);
        }
        huntMarkers.clear();
        
        // Generate new hunt markers
        for (int i = 0; i < HUNT_MARKER_COUNT; i++) {
            // Generate random distance and angle
            double distance = HUNT_MARKER_MIN_DISTANCE + 
                (Math.random() * (HUNT_MARKER_MAX_DISTANCE - HUNT_MARKER_MIN_DISTANCE));
            double angle = Math.random() * 2 * Math.PI; // Random angle in radians
            
            // Calculate new position
            double lat = center.getLatitude() + (distance / 111320.0 * Math.cos(angle));
            double lon = center.getLongitude() + (distance / (111320.0 * Math.cos(Math.toRadians(center.getLatitude()))) * Math.sin(angle));
            
            // Create marker
            Marker huntMarker = new Marker(mapView);
            huntMarker.setPosition(new GeoPoint(lat, lon));
            huntMarker.setTitle("Hunt Item " + (i + 1));
            huntMarker.setSnippet("Tap to collect!");
            
            // Use a simple bitmap for hunt markers
            try {
                Bitmap markerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_place);
                huntMarker.setIcon(new BitmapDrawable(getResources(), markerBitmap));
                huntMarker.setTitle("Hunt Item");
            } catch (Exception e) {
                // Fallback to default marker if custom icon fails
                huntMarker.setIcon(getResources().getDrawable(android.R.drawable.ic_dialog_map));
            }
            huntMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            
            // Add click listener
            huntMarker.setOnMarkerClickListener((marker, mapView) -> {
                // Remove this marker
                mapView.getOverlays().remove(marker);
                mapView.invalidate();
                
                // Generate a new one in a different location
                generateHuntMarkers(center);
                
                // Show collected message
                Toast.makeText(this, "Item collected! New items have appeared!", Toast.LENGTH_SHORT).show();
                return true;
            });
            
            huntMarkers.add(huntMarker);
            mapView.getOverlays().add(huntMarker);
        }
        
        mapView.invalidate();
    }

    @Override
    public void onAvatarSelected(String name, String avatarResource) {
        currentAvatar = avatarResource + ".png";
        Prefs.saveAvatar(this, currentAvatar);
        updateAvatarButton();
        updateUserMarker();
        
        // Update player info in the application
        ScavengerHuntApplication app = (ScavengerHuntApplication) getApplication();
        app.createPlayer(name, avatarResource);
    }
}
