package com.adventure.solo.ui.ar;

import android.Manifest;
import android.content.Context; // For onAttach
import android.content.pm.PackageManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent; // For setOnTouchListener
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

// import com.adventure.solo.R; // Auto-imported by IDE typically
import com.adventure.solo.databinding.FragmentArSceneBinding;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import android.location.Location;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ARSceneFragment extends Fragment {

    private static final String TAG = "ARSceneFragment";
    private static final int CAMERA_PERMISSION_CODE = 0; // Can be combined if handling is generic
    private static final int LOCATION_PERMISSION_CODE = 1; // Can be combined

    private FragmentArSceneBinding binding;
    private Session session;
    private GLSurfaceView glSurfaceView;
    private DisplayRotationHelper displayRotationHelper;
    private MainRenderer mainRenderer;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location currentLocation;

    private double targetLatitude;
    private double targetLongitude;
    private long clueId; // Added
    private int rewardPoints; // Added

    private boolean objectVisible = false;
    private static final float PROXIMITY_RADIUS_METERS = 20.0f;

    private ARObjectInteractionListener interactionListener; // Added

    // Required empty public constructor
    public ARSceneFragment() {}

    public static ARSceneFragment newInstance(double lat, double lon, long clueId, int rewardPoints) {
        ARSceneFragment fragment = new ARSceneFragment();
        Bundle args = new Bundle();
        args.putDouble("targetLatitude", lat);
        args.putDouble("targetLongitude", lon);
        args.putLong("clueId", clueId); // Added
        args.putInt("rewardPoints", rewardPoints); // Added
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Try to attach to parent fragment first, then activity
        if (getParentFragment() instanceof ARObjectInteractionListener) {
            interactionListener = (ARObjectInteractionListener) getParentFragment();
            Log.d(TAG, "Attached listener from parent fragment.");
        } else if (context instanceof ARObjectInteractionListener) {
            interactionListener = (ARObjectInteractionListener) context;
            Log.d(TAG, "Attached listener from context (Activity).");
        } else {
            Log.e(TAG, context.toString() + " or parent fragment must implement ARObjectInteractionListener");
            throw new RuntimeException(context.toString() + " or parent fragment must implement ARObjectInteractionListener");
        }
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            targetLatitude = getArguments().getDouble("targetLatitude");
            targetLongitude = getArguments().getDouble("targetLongitude");
            clueId = getArguments().getLong("clueId");
            rewardPoints = getArguments().getInt("rewardPoints");
            Log.d(TAG, "ARSceneFragment created for Clue ID: " + clueId + " at " + targetLatitude + "," + targetLongitude);
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentArSceneBinding.inflate(inflater, container, false);
        glSurfaceView = binding.glSurfaceView;

        displayRotationHelper = new DisplayRotationHelper(requireContext());
        mainRenderer = new MainRenderer(requireContext(), displayRotationHelper, () -> {
            // This Runnable is called by MainRenderer's handleTap()
            Log.d(TAG, "Tap event received from MainRenderer. Object visible: " + objectVisible);
            if (objectVisible && interactionListener != null) {
                Log.d(TAG, "Object collected: Clue ID " + clueId + ", Points: " + rewardPoints);
                objectVisible = false; // Hide immediately prevent re-collection
                if(mainRenderer != null) mainRenderer.setObjectVisible(false);
                interactionListener.onARElementCollected(clueId, rewardPoints);

                // Pop backstack to close AR view after collection
                if (getParentFragmentManager() != null) {
                     getParentFragmentManager().popBackStack();
                } else {
                    Log.w(TAG, "ParentFragmentManager is null, cannot popBackStack.");
                }
            } else {
                Log.d(TAG, "Tap ignored. Object not visible or no listener. Visible: " + objectVisible + ", Listener: " + (interactionListener != null));
            }
        });

        glSurfaceView.setPreserveEGLContextOnPause(true);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        glSurfaceView.setRenderer(mainRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        glSurfaceView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (mainRenderer != null) {
                    mainRenderer.handleTap();
                }
                return true;
            }
            return false;
        });

        setupLocationCallback();
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
        if (!checkAndRequestPermissions()) {
            Log.d(TAG, "Permissions not granted yet in onResume.");
            return;
        }
        Log.d(TAG, "Permissions granted, starting location updates.");
        startLocationUpdates();

        if (session == null) {
            Log.d(TAG, "ARCore session is null, attempting to create.");
            try {
                ArCoreApk.InstallStatus installStatus = ArCoreApk.getInstance().requestInstall(requireActivity(), true);
                if (installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                    Log.d(TAG, "ARCore installation requested. Will return and wait.");
                    return;
                }
                session = new Session(requireContext());
                Log.d(TAG, "ARCore session created.");
            } catch (Exception e) {
                handleSessionCreationException(e);
                return;
            }
        }

        try {
            Log.d(TAG, "Resuming ARCore session.");
            session.resume();
            if(mainRenderer!=null) mainRenderer.setSession(session);
        } catch (CameraNotAvailableException e) {
            Log.e(TAG, "Camera not available during onResume", e);
            Toast.makeText(requireContext(), "Camera not available. Please restart the app.", Toast.LENGTH_LONG).show();
            session = null; // Invalidate session
            return;
        }
        if(glSurfaceView!=null) glSurfaceView.onResume();
        if(displayRotationHelper!=null) displayRotationHelper.onResume();
        Log.d(TAG, "onResume completed.");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
        stopLocationUpdates();
        if (session != null) {
            Log.d(TAG, "Pausing ARCore session.");
            if(displayRotationHelper!=null) displayRotationHelper.onPause();
            if(glSurfaceView!=null) glSurfaceView.onPause();
            session.pause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called");
        if (session != null) {
            Log.d(TAG, "Closing ARCore session.");
            session.close();
            session = null;
        }
        mainRenderer = null;
        displayRotationHelper = null;
        glSurfaceView = null;
        binding = null;
    }

    private boolean checkAndRequestPermissions() {
        boolean cameraPermissionGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean locationPermissionGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        java.util.List<String> permissionsToRequest = new java.util.ArrayList<>();
        if (!cameraPermissionGranted) {
            permissionsToRequest.add(Manifest.permission.CAMERA);
        }
        if (!locationPermissionGranted) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!permissionsToRequest.isEmpty()) {
            Log.d(TAG, "Requesting permissions: " + permissionsToRequest);
            ActivityCompat.requestPermissions(requireActivity(), permissionsToRequest.toArray(new String[0]), LOCATION_PERMISSION_CODE); // Using one code for simplicity
            return false;
        }
        Log.d(TAG, "All necessary permissions already granted.");
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: requestCode " + requestCode);

        // We now check specific permissions granted status after request.
        boolean cameraGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean locationGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (cameraGranted && locationGranted) {
            Log.d(TAG, "Both camera and location permissions granted after request.");
            // onResume will be called by the lifecycle if fragment was not resumed.
            // If it was already resumed, but permissions were missing, we might need to trigger setup again.
            // However, current onResume logic handles re-check.
        } else {
            Log.w(TAG, "Permissions denied. Camera: " + cameraGranted + ", Location: " + locationGranted);
            Toast.makeText(requireContext(), "Camera and Location permissions are essential for AR features.", Toast.LENGTH_LONG).show();
            // Consider guiding user to settings or disabling AR features.
        }
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    currentLocation = location;
                    checkProximity();
                }
            }
        };
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Attempted to start location updates without permission.");
            return;
        }
        if (fusedLocationClient == null) {
             fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        }
        if (locationCallback == null) { // Should have been setup in onCreateView
            setupLocationCallback();
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000); // 5 seconds
        locationRequest.setFastestInterval(2000); // 2 seconds
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        Log.d(TAG, "Requesting location updates.");
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, requireActivity().getMainLooper());
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            Log.d(TAG, "Stopping location updates.");
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private void checkProximity() {
        if (currentLocation != null && targetLatitude != 0 && targetLongitude != 0) {
            Location targetLocation = new Location("");
            targetLocation.setLatitude(targetLatitude);
            targetLocation.setLongitude(targetLongitude);

            float distance = currentLocation.distanceTo(targetLocation);
            // Log.d(TAG, "Distance to target (Clue ID " + clueId + "): " + distance + " meters");

            boolean shouldBeVisible = distance < PROXIMITY_RADIUS_METERS;
            if (shouldBeVisible && !objectVisible) {
                objectVisible = true;
                if(mainRenderer!=null) mainRenderer.setObjectVisible(true);
                Log.i(TAG, "Object for Clue ID " + clueId + " is now IN RANGE.");
                // Toast.makeText(getContext(), "Object in range!", Toast.LENGTH_SHORT).show();
            } else if (!shouldBeVisible && objectVisible) {
                objectVisible = false;
                if(mainRenderer!=null) mainRenderer.setObjectVisible(false);
                Log.i(TAG, "Object for Clue ID " + clueId + " is now OUT OF RANGE.");
                // Toast.makeText(getContext(), "Object out of range", Toast.LENGTH_SHORT).show();
            }
        } else {
             if (objectVisible) {
                objectVisible = false;
                if(mainRenderer!=null) mainRenderer.setObjectVisible(false);
                Log.i(TAG, "No current location, hiding object for Clue ID " + clueId);
             }
        }
    }

    private void handleSessionCreationException(Exception e) {
        String message;
        if (e instanceof UnavailableApkTooOldException) {
            message = "Please update ARCore.";
        } else if (e instanceof UnavailableDeviceNotCompatibleException) {
            message = "This device does not support AR.";
        } else if (e instanceof UnavailableSdkTooOldException) {
            message = "Please update this app.";
        } else if (e instanceof UnavailableUserDeclinedInstallationException) {
            message = "Please install ARCore to use this feature.";
        } else {
            message = "Failed to create AR session: " + e.getMessage();
        }
        Log.e(TAG, "Error creating AR session: " + message, e);
        if(getContext() != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        }
    }
}
