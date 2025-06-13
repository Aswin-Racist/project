package com.adventure.solo.repository;

import com.adventure.solo.model.Quest;
import com.adventure.solo.database.QuestDao;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;
import android.util.Log; // For logging

@Singleton
public class QuestRepository {
    private static final String TAG = "QuestRepository";
    private final QuestDao questDao;
    private final ExecutorService executorService;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    // Callback interface for asynchronous operations
    public interface QuestRepoCallback<T> {
        void onComplete(T result);
        void onError(Exception e); // Optional error handling
    }

    @Inject
    public QuestRepository(QuestDao questDao) {
        this.questDao = questDao;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public long insert(Quest quest) {
        return questDao.insert(quest);
    }

    public void update(Quest quest) {
        questDao.update(quest);
    }

    public void delete(long questId) {
        questDao.delete(questId);
    }

    public Quest getQuest(long questId) {
        return questDao.getQuest(questId);
    }

    public List<Quest> getActiveQuests() {
        return questDao.getActiveQuests();
    }

    public List<Quest> getAllQuests() {
        return questDao.getAllQuests();
    }

    public void updateQuestCompletedStatus(long questId, boolean completed) {
        questDao.updateQuestCompletedStatus(questId, completed);
    }

    // New asynchronous version of getAllQuests
    public void getAllQuests(QuestRepoCallback<List<Quest>> callback) {
        executorService.execute(() -> {
            try {
                List<Quest> quests = questDao.getAllQuests(); // This is the synchronous DAO method
                mainThreadHandler.post(() -> callback.onComplete(quests));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching all quests from database", e);
                mainThreadHandler.post(() -> callback.onError(e));
            }
        });
    }

    // Keep existing synchronous getAllQuests() if it's used elsewhere,
    // or rename it to getAllQuestsSync() for clarity.
    // For this subtask, the new async one is primary for ViewModel.
    public List<Quest> getAllQuestsNonLiveData() { // Renamed for clarity
        return questDao.getAllQuests();
    }

    // Similar async wrappers can be made for other DAO methods if needed by ViewModels
    public void getQuestById(long questId, QuestRepoCallback<Quest> callback) {
        executorService.execute(() -> {
            try {
                Quest quest = questDao.getQuest(questId); // Renamed from getQuest to getQuestById in DAO if that change was made
                mainThreadHandler.post(() -> callback.onComplete(quest));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching quest by ID: " + questId, e);
                mainThreadHandler.post(() -> callback.onError(e));
            }
        });
    }
}