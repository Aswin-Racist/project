package com.adventure.solo.repository;

import com.adventure.solo.database.PlayerProfileDao;
import com.adventure.solo.model.PlayerProfile;
import java.util.List; // For future methods that might return lists
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;


@Singleton
public class PlayerProfileRepository {
    private final PlayerProfileDao playerProfileDao;
    private final ExecutorService executorService; // For background operations
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());


    // Define a callback interface for async operations
    public interface PlayerProfileCallback<T> {
        void onComplete(T result);
    }

    @Inject
    public PlayerProfileRepository(PlayerProfileDao playerProfileDao) {
        this.playerProfileDao = playerProfileDao;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void insertOrUpdate(PlayerProfile playerProfile, PlayerProfileCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                playerProfileDao.insertOrUpdate(playerProfile);
                if (callback != null) mainThreadHandler.post(() -> callback.onComplete(null));
            } catch (Exception e) {
                // Handle error, maybe pass to callback
                android.util.Log.e("PlayerProfileRepo", "Error inserting/updating profile", e);
                if (callback != null) mainThreadHandler.post(() -> callback.onComplete(null)); // Or an error state
            }
        });
    }

    public void getByFirebaseUid(String uid, PlayerProfileCallback<PlayerProfile> callback) {
        executorService.execute(() -> {
            try {
                PlayerProfile profile = playerProfileDao.getByFirebaseUid(uid);
                mainThreadHandler.post(() -> callback.onComplete(profile));
            } catch (Exception e) {
                android.util.Log.e("PlayerProfileRepo", "Error getting profile by UID", e);
                mainThreadHandler.post(() -> callback.onComplete(null)); // Or an error state
            }
        });
    }

    public void getPlayersByTeamId(String teamId, PlayerProfileCallback<List<PlayerProfile>> callback) {
        executorService.execute(() -> {
            try {
                List<PlayerProfile> profiles = playerProfileDao.getPlayersByTeamId(teamId);
                mainThreadHandler.post(() -> callback.onComplete(profiles));
            } catch (Exception e) {
                android.util.Log.e("PlayerProfileRepo", "Error getting profiles by team ID", e);
                mainThreadHandler.post(() -> callback.onComplete(null)); // Or an error state
            }
        });
    }

    public void updatePlayerProfile(PlayerProfile playerProfile, PlayerProfileCallback<Void> callback) {
         executorService.execute(() -> {
            try {
                playerProfileDao.update(playerProfile);
                if (callback != null) mainThreadHandler.post(() -> callback.onComplete(null));
            } catch (Exception e) {
                android.util.Log.e("PlayerProfileRepo", "Error updating profile", e);
                if (callback != null) mainThreadHandler.post(() -> callback.onComplete(null)); // Or an error state
            }
        });
    }

    public void updateTeamIdForPlayer(String firebaseUid, String newTeamId, PlayerProfileCallback<Boolean> callback) {
        executorService.execute(() -> {
            try {
                PlayerProfile profile = playerProfileDao.getByFirebaseUid(firebaseUid);
                if (profile != null) {
                    profile.setTeamId(newTeamId); // Use setter if available
                    playerProfileDao.update(profile);
                    mainThreadHandler.post(() -> callback.onComplete(true));
                } else {
                    android.util.Log.e("PlayerProfileRepo", "Profile not found for UID: " + firebaseUid + " when trying to update teamId.");
                    mainThreadHandler.post(() -> callback.onComplete(false)); // Profile not found
                }
            } catch (Exception e) {
                android.util.Log.e("PlayerProfileRepo", "Error updating team ID for player " + firebaseUid, e);
                mainThreadHandler.post(() -> callback.onComplete(false));
            }
        });
    }

    // Renaming getByFirebaseUid to getPlayerProfile to match ViewModel usage, functionally same
    public void getPlayerProfile(String firebaseUid, PlayerProfileCallback<PlayerProfile> callback) {
        getByFirebaseUid(firebaseUid, callback);
    }

    public void addExperience(String firebaseUid, int xpToAdd, PlayerProfileCallback<Boolean> callback) {
        executorService.execute(() -> {
            try {
                PlayerProfile profile = playerProfileDao.getByFirebaseUid(firebaseUid);
                if (profile != null) {
                    profile.individualXP += xpToAdd;
                    playerProfileDao.update(profile);
                    mainThreadHandler.post(() -> callback.onComplete(true));
                } else {
                    mainThreadHandler.post(() -> callback.onComplete(false));
                }
            } catch (Exception e) {
                android.util.Log.e("PlayerProfileRepo", "Error adding experience for " + firebaseUid, e);
                mainThreadHandler.post(() -> callback.onComplete(false));
            }
        });
    }

    public void addCoins(String firebaseUid, int coinsToAdd, PlayerProfileCallback<Boolean> callback) {
        executorService.execute(() -> {
            try {
                PlayerProfile profile = playerProfileDao.getByFirebaseUid(firebaseUid);
                if (profile != null) {
                    profile.coins += coinsToAdd;
                    playerProfileDao.update(profile);
                    mainThreadHandler.post(() -> callback.onComplete(true));
                } else {
                    mainThreadHandler.post(() -> callback.onComplete(false));
                }
            } catch (Exception e) {
                android.util.Log.e("PlayerProfileRepo", "Error adding coins for " + firebaseUid, e);
                mainThreadHandler.post(() -> callback.onComplete(false));
            }
        });
    }

    public void updateStaminaAndCoins(String firebaseUid, int newStamina, int newCoins, PlayerProfileCallback<Boolean> callback) {
        executorService.execute(() -> {
            try {
                PlayerProfile profile = playerProfileDao.getByFirebaseUid(firebaseUid);
                if (profile != null) {
                    profile.setStamina(newStamina);
                    profile.setCoins(newCoins);
                    playerProfileDao.update(profile);
                    mainThreadHandler.post(() -> callback.onComplete(true));
                } else {
                    android.util.Log.e("PlayerProfileRepo", "Profile not found for UID: " + firebaseUid + " when updating stamina/coins.");
                    mainThreadHandler.post(() -> callback.onComplete(false));
                }
            } catch (Exception e) {
                android.util.Log.e("PlayerProfileRepo", "Error updating stamina/coins for " + firebaseUid, e);
                mainThreadHandler.post(() -> callback.onComplete(false));
            }
        });
    }
}
