package com.demo.map;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.demo.map.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment {

    private static final String TAG = "MapFragment";
    private MapView mapView;
    private IMapController mapController;
    private MyLocationNewOverlay myLocationOverlay;
    private ProgressBar progressBar;
    private Button btnSearch;

    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 2; // Different from Activity's code

    public MapFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            // Initialize osmdroid configuration
            Context ctx = requireActivity().getApplicationContext();
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
        } catch (Exception e) {
            Log.e(TAG, "Error initializing map configuration: " + e.getMessage(), e);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        
        try {
            mapView = view.findViewById(R.id.mapViewFragment);
            progressBar = view.findViewById(R.id.progressBarFragment);
            btnSearch = view.findViewById(R.id.btnSearchFragment);
            
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
            } else {
                Toast.makeText(requireContext(), "Error initializing map view", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing map view: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error initializing map view: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        
        return view;
    }

    private void initializeLocationOverlay() {
        if (ContextCompat.checkSelfPermission(requireContext(), 
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), mapView);
            myLocationOverlay.enableMyLocation();
            myLocationOverlay.enableFollowLocation();
            mapView.getOverlays().add(myLocationOverlay);
            
            // Center on location when available
            myLocationOverlay.runOnFirstFix(() -> requireActivity().runOnUiThread(() -> {
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
            if (ContextCompat.checkSelfPermission(requireContext(), 
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                BoundingBox boundingBox = mapView.getBoundingBox();
                fetchPOIs(boundingBox);
            } else {
                requestPermissionsIfNecessary(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }
        });
    }

    private void fetchPOIs(BoundingBox boundingBox) {
        String overpassQuery = "[out:json][timeout:25];(" +
                "node[\"amenity\"](" + boundingBox.getLatSouth() + "," + boundingBox.getLonWest() + "," + boundingBox.getLatNorth() + "," + boundingBox.getLonEast() + ");" +
                ");out body;>;out skel qt;";
        String overpassUrl = "https://overpass-api.de/api/interpreter?data=" + overpassQuery;
        Log.d(TAG, "Overpass URL: " + overpassUrl);
        new FetchPOIsTask().execute(overpassUrl);
    }

    private class FetchPOIsTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            // Clear existing markers except MyLocationOverlay
            List<org.osmdroid.views.overlay.Overlay> overlaysToRemove = new ArrayList<>();
            for (org.osmdroid.views.overlay.Overlay overlay : mapView.getOverlays()) {
                if (!(overlay instanceof MyLocationNewOverlay)) {
                    overlaysToRemove.add(overlay);
                }
            }
            mapView.getOverlays().removeAll(overlaysToRemove);
            mapView.invalidate();
        }

        @Override
        protected String doInBackground(String... urls) {
            if (urls.length < 1 || urls[0] == null) return null;
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
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            if (response != null) {
                Log.d(TAG, "Overpass Response: " + response);
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONArray elements = jsonResponse.getJSONArray("elements");
                    for (int i = 0; i < elements.length(); i++) {
                        JSONObject element = elements.getJSONObject(i);
                        if (element.has("lat") && element.has("lon")) {
                            double lat = element.getDouble("lat");
                            double lon = element.getDouble("lon");
                            String name = element.optJSONObject("tags") != null ? element.optJSONObject("tags").optString("name", "N/A") : "N/A";
                            String amenityType = element.optJSONObject("tags") != null ? element.optJSONObject("tags").optString("amenity", "Unknown") : "Unknown";

                            HuntMarker poiMarker = new HuntMarker(mapView, new GeoPoint(lat, lon), name, "Type: " + amenityType, amenityType, name);
                            mapView.getOverlays().add(poiMarker);
                        }
                    }
                    mapView.invalidate();
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Found " + elements.length() + " POIs", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing POIs: " + e.getMessage(), e);
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Error parsing POIs", Toast.LENGTH_SHORT).show();
                }
            } else {
                if (getContext() != null)
                    Toast.makeText(getContext(), "Failed to fetch POIs", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            // Check if permission is granted
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                 permissionsToRequest.add(permissions[i]);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    requireActivity(),
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
            if (ContextCompat.checkSelfPermission(requireContext(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            requestPermissions( // Use requestPermissions for Fragment
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }
}