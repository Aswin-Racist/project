package com.adventure.solo.repository;

import com.adventure.solo.database.ClueProgressDao;
import com.adventure.solo.model.ClueProgress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;
import javax.inject.Inject;
import javax.inject.Singleton;
import android.util.Log;

@Singleton
public class ClueProgressRepository {
    private static final String TAG = "ClueProgressRepository";
    private final ClueProgressDao clueProgressDao;
    private final ExecutorService executorService;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    public interface ClueProgressCallback<T> { void onComplete(T result); void onError(Exception e); }

    @Inject
    public ClueProgressRepository(ClueProgressDao clueProgressDao) {
        this.clueProgressDao = clueProgressDao;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void insertOrUpdate(ClueProgress cp, ClueProgressCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                clueProgressDao.insertOrUpdate(cp);
                if(callback != null) mainThreadHandler.post(() -> callback.onComplete(null));
            } catch (Exception e) {
                Log.e(TAG, "Error in insertOrUpdate: " + e.getMessage(), e);
                if(callback != null) mainThreadHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void getClueProgress(long actualClueId, String teamId, ClueProgressCallback<ClueProgress> callback) {
        executorService.execute(() -> {
            try {
                ClueProgress cp = clueProgressDao.getClueProgress(actualClueId, teamId);
                mainThreadHandler.post(() -> callback.onComplete(cp));
            } catch (Exception e) {
                Log.e(TAG, "Error in getClueProgress: " + e.getMessage(), e);
                mainThreadHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void getProgressForQuestByTeam(long questId, String teamId, ClueProgressCallback<List<ClueProgress>> callback) {
        executorService.execute(() -> {
            try {
                // Assuming ClueProgress.questId is long, and DAO method expects long.
                List<ClueProgress> progresses = clueProgressDao.getProgressForQuestByTeam(questId, teamId);
                mainThreadHandler.post(() -> callback.onComplete(progresses));
            } catch (Exception e) {
                Log.e(TAG, "Error in getProgressForQuestByTeam: " + e.getMessage(), e);
                mainThreadHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void updateDiscovered(long actualClueId, String teamId, boolean discovered, String playerId, ClueProgressCallback<Void> callback) {
         executorService.execute(() -> {
            try {
                clueProgressDao.updateDiscovered(actualClueId, teamId, discovered, playerId);
                if(callback != null) mainThreadHandler.post(() -> callback.onComplete(null));
            } catch (Exception e) {
                Log.e(TAG, "Error in updateDiscovered: " + e.getMessage(), e);
                if(callback != null) mainThreadHandler.post(() -> callback.onError(e));
            }
        });
    }
}
