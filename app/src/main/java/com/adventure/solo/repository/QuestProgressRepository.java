package com.adventure.solo.repository;

import com.adventure.solo.database.QuestProgressDao;
import com.adventure.solo.model.QuestProgress;
import com.adventure.solo.model.QuestStatus;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;
import javax.inject.Inject;
import javax.inject.Singleton;
import android.util.Log;

@Singleton
public class QuestProgressRepository {
    private static final String TAG = "QuestProgressRepository";
    private final QuestProgressDao questProgressDao;
    private final ExecutorService executorService;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    public interface QuestProgressCallback<T> { void onComplete(T result); void onError(Exception e); }

    @Inject
    public QuestProgressRepository(QuestProgressDao questProgressDao) {
        this.questProgressDao = questProgressDao;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void insertOrUpdate(QuestProgress qp, QuestProgressCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                questProgressDao.insertOrUpdate(qp);
                if(callback != null) mainThreadHandler.post(() -> callback.onComplete(null));
            } catch (Exception e) {
                Log.e(TAG, "Error in insertOrUpdate: " + e.getMessage(), e);
                if(callback != null) mainThreadHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void getQuestProgress(long questId, String teamId, QuestProgressCallback<QuestProgress> callback) {
        executorService.execute(() -> {
            try {
                // Assuming questId in QuestProgress is long, and DAO method expects long.
                QuestProgress qp = questProgressDao.getQuestProgress(questId, teamId);
                mainThreadHandler.post(() -> callback.onComplete(qp));
            } catch (Exception e) {
                Log.e(TAG, "Error in getQuestProgress: " + e.getMessage(), e);
                mainThreadHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void getProgressForTeam(String teamId, QuestProgressCallback<List<QuestProgress>> callback) {
         executorService.execute(() -> {
            try {
                List<QuestProgress> progresses = questProgressDao.getProgressForTeam(teamId);
                mainThreadHandler.post(() -> callback.onComplete(progresses));
            } catch (Exception e) {
                Log.e(TAG, "Error in getProgressForTeam: " + e.getMessage(), e);
                mainThreadHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void updateStatus(long questId, String teamId, QuestStatus status, String playerId, QuestProgressCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                // Assuming questId in QuestProgress is long, and DAO method expects long.
                questProgressDao.updateStatus(questId, teamId, status, playerId);
                if(callback != null) mainThreadHandler.post(() -> callback.onComplete(null));
            } catch (Exception e) {
                Log.e(TAG, "Error in updateStatus: " + e.getMessage(), e);
                if(callback != null) mainThreadHandler.post(() -> callback.onError(e));
            }
        });
    }
}
