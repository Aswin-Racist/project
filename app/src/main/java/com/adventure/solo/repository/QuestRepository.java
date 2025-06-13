package com.adventure.solo.repository;

import com.adventure.solo.database.ClueDao; // Needed to insert clues
import com.adventure.solo.database.QuestDao;
import com.adventure.solo.model.Clue; // Needed for List<Clue>
import com.adventure.solo.model.Quest;
// Using local QuestRepoCallback instead of QuestManager.QuestManagerCallback
// import com.adventure.solo.service.QuestManager;

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
    private final ClueDao clueDao; // Added for inserting clues with quest
    private final ExecutorService executorService;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    // Callback interface for asynchronous operations
    public interface QuestRepoCallback<T> { void onComplete(T result); void onError(Exception e); }


    @Inject
    public QuestRepository(QuestDao questDao, ClueDao clueDao) { // Added ClueDao
        this.questDao = questDao;
        this.clueDao = clueDao; // Added
        this.executorService = Executors.newSingleThreadExecutor();
    }

    // insert(Quest) was renamed insertQuestAndGetId in DAO and returns long
    // This repository method can be void or return long based on need.
    // Let's assume QuestManager needs the ID back, so it should match the DAO.
    public long insertQuestAndGetId(Quest quest) { // Renamed to match DAO
        return questDao.insertQuestAndGetId(quest);
    }

    public void updateQuest(Quest quest) { // Renamed to match DAO
        questDao.updateQuest(quest);
    }

    // delete(long questId) was removed in new DAO, if needed, re-add to DAO
    // public void delete(long questId) {
    //    questDao.delete(questId);
    // }

    public Quest getQuestById(long questId) { // Renamed to match DAO, synchronous version
        try {
            return questDao.getQuestById(questId);
        } catch (Exception e) {
            Log.e(TAG, "Error getting quest by ID sync", e);
            return null;
        }
    }

    // getActiveQuests() was removed in new DAO, if needed, re-add to DAO
    // public List<Quest> getActiveQuests() {
    //    return questDao.getActiveQuests();
    // }

    public List<Quest> getAllQuestsNonLiveData() { // Name matches new DAO method
        return questDao.getAllQuestsNonLiveData();
    }

    public void updateQuestCompletedStatus(long questId, boolean completed) { // Synchronous, called from QuestManager's bg thread
        try {
            questDao.updateQuestCompletedStatus(questId, completed);
        } catch (Exception e) {
            Log.e(TAG, "Error updating quest completed status", e);
        }
    }

    // Async version of getAllQuests
    public void getAllQuests(QuestRepoCallback<List<Quest>> callback) {
        executorService.execute(() -> {
            try {
                List<Quest> quests = questDao.getAllQuestsNonLiveData(); // Uses the renamed DAO method
                mainThreadHandler.post(() -> callback.onComplete(quests));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching all quests from database async", e);
                mainThreadHandler.post(() -> callback.onError(e));
            }
        });
    }

    // Keep existing synchronous getAllQuests() if it's used elsewhere,
    // or rename it to getAllQuestsSync() for clarity.
    // For this subtask, the new async one is primary for ViewModel.
    public List<Quest> getAllQuestsNonLiveDataOld() { // Kept original synchronous getAllQuests, renamed
        return questDao.getAllQuestsNonLiveData(); // Assuming this still points to the old getAllQuests if needed
    }

    // Async version of getQuestById
    public void getQuestById(long questId, QuestRepoCallback<Quest> callback) {
        executorService.execute(() -> {
            try {
                Quest quest = questDao.getQuestById(questId); // Uses renamed DAO method
                mainThreadHandler.post(() -> callback.onComplete(quest));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching quest by ID async: " + questId, e);
                mainThreadHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void insertQuestAndCluesForDebug(Quest quest, List<Clue> clues, QuestRepoCallback<Long> callback) {
        executorService.execute(() -> {
            try {
                long generatedQuestId = questDao.insertQuestAndGetId(quest);
                if (generatedQuestId > 0 && clues != null && !clues.isEmpty()) {
                    for (Clue clue : clues) {
                        clue.setQuestId(generatedQuestId);
                    }
                    clueDao.insertAllClues(clues); // Assumes ClueDao has insertAllClues
                }
                mainThreadHandler.post(() -> callback.onComplete(generatedQuestId));
            } catch (Exception e) {
                Log.e(TAG, "Error inserting quest and clues for debug", e);
                mainThreadHandler.post(() -> callback.onError(e));
            }
        });
    }
}