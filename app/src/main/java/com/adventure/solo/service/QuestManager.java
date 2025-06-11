package com.adventure.solo.service;

import android.location.Location;
import com.adventure.solo.model.Quest;
import com.adventure.solo.model.Clue;
import com.adventure.solo.repository.QuestRepository;
import com.adventure.solo.repository.ClueRepository;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;
import android.util.Log; // For logging
import java.util.List; // For List
import java.util.Collections; // For Collections.sort if needed
import java.util.Comparator; // For Comparator if needed


@Singleton
public class QuestManager {
    private final QuestGenerator questGenerator; // Assuming this is injected or handled elsewhere
    private final QuestRepository questRepository;
    private final ClueRepository clueRepository;
    private static final float PROXIMITY_RADIUS = 50; // meters - This seems like a duplicate, ARSceneFragment has its own.
    private static final int DEFAULT_CLUES_PER_QUEST = 5; // Assuming this is used by QuestGenerator

    private final ExecutorService executorService;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    @Inject
    public QuestManager(QuestGenerator questGenerator, // Keep if QuestGenerator is part of Hilt DI
                       QuestRepository questRepository,
                       ClueRepository clueRepository) {
        this.questGenerator = questGenerator;
        this.questRepository = questRepository;
        this.clueRepository = clueRepository;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public interface QuestManagerCallback<T> {
        void onComplete(T result);
        // void onError(Exception e); // Optional: For more detailed error handling
    }

    // generateNewQuest and other existing methods from your previous file would be here...
    // For brevity, I'm focusing on adding the new ones and modifying markClueAsDiscovered.
    // Ensure your existing generateNewQuest, getActiveQuests etc. are preserved.
    public Quest generateNewQuest(Location userLocation, int difficulty) {
        Quest quest = questGenerator.generateQuest(userLocation, difficulty);
        long questId = questRepository.insert(quest);
        quest.setId(questId);

        List<Clue> clues = questGenerator.generateClues(quest, DEFAULT_CLUES_PER_QUEST);
        clueRepository.insertAll(clues);

        return quest;
    }

    public List<Quest> getActiveQuests() {
        return questRepository.getActiveQuests();
    }

    public List<Clue> getQuestClues(long questId) {
        return clueRepository.getCluesForQuest(questId);
    }

    public Clue getNextClue(long questId) {
        return clueRepository.getNextUndiscoveredClue(questId);
    }

    public boolean checkProximityToClue(Location userLocation, Clue clue) {
        float[] results = new float[1];
        Location.distanceBetween(
            userLocation.getLatitude(), userLocation.getLongitude(),
            clue.getTargetLatitude(), clue.getTargetLongitude(),
            results
        );
        return results[0] <= PROXIMITY_RADIUS;
    }

    public void markClueAsDiscovered(Clue clue) {
        clue.setDiscovered(true);
        clueRepository.update(clue);

        // Check if all clues are discovered
        List<Clue> undiscoveredClues = clueRepository.getUndiscoveredClues(clue.getQuestId());
        if (undiscoveredClues.isEmpty()) {
            Quest quest = questRepository.getQuest(clue.getQuestId());
            quest.setCompleted(true);
            questRepository.update(quest);
        }
    }

    public boolean isQuestComplete(long questId) {
        Quest quest = questRepository.getQuest(questId);
        return quest != null && quest.isCompleted();
    }

    public void abandonQuest(long questId) {
        questRepository.delete(questId);
        // Clues will be automatically deleted due to CASCADE delete in Room
    }

    public void markClueAsDiscovered(long clueId, QuestManagerCallback<Boolean> callback) {
        executorService.execute(() -> {
            try {
                clueRepository.updateDiscoveredStatus(clueId, true);
                // Assuming success if no exception
                // Additionally, check if this was the last clue for the quest
                Clue collectedClue = clueRepository.getClueById(clueId);
                if (collectedClue != null) {
                    checkAndCompleteQuest(collectedClue.getQuestId()); // Refactored completion check
                } else {
                    Log.w("QuestManager", "Collected clue with ID " + clueId + " not found in DB. Cannot check for quest completion.");
                }
                mainThreadHandler.post(() -> callback.onComplete(true));
            } catch (Exception e) {
                Log.e("QuestManager", "Error marking clue discovered: " + clueId, e);
                mainThreadHandler.post(() -> callback.onComplete(false));
            }
        });
    }

    private void checkAndCompleteQuest(long questId) {
        // This method is called on background thread
        List<Clue> cluesForQuest = clueRepository.getCluesByQuestIdNonLiveData(questId);
        boolean allDiscovered = true;
        if (cluesForQuest == null || cluesForQuest.isEmpty()) {
            // This might be an error or a quest with no clues.
            // If a quest must have clues to be completable, this could be an error.
            // For now, assume a quest with no clues isn't 'allDiscovered' in this context.
            allDiscovered = false;
            Log.w("QuestManager", "No clues found for quest ID " + questId + " when checking for completion.");
        } else {
            for (Clue clue : cluesForQuest) {
                if (!clue.isDiscovered()) {
                    allDiscovered = false;
                    break;
                }
            }
        }
        if (allDiscovered) {
            questRepository.updateQuestCompletedStatus(questId, true);
            Log.i("QuestManager", "Quest " + questId + " marked as completed.");
        }
    }

    public void getQuestDetails(long questId, QuestManagerCallback<Quest> callback) {
        executorService.execute(() -> {
            try {
                Quest quest = questRepository.getQuestById(questId);
                mainThreadHandler.post(() -> callback.onComplete(quest));
            } catch (Exception e) {
                Log.e("QuestManager", "Error getting quest details for ID: " + questId, e);
                mainThreadHandler.post(() -> callback.onComplete(null)); // Or trigger onError
            }
        });
    }

    public void getNextClueForQuest(long questId, long lastCollectedClueId, QuestManagerCallback<Clue> callback) {
         executorService.execute(() -> {
            try {
                List<Clue> clues = clueRepository.getCluesByQuestIdNonLiveData(questId); // Assumes sorted by sequence
                Clue nextClue = null;
                if (clues != null && !clues.isEmpty()) {
                    if (lastCollectedClueId == 0) { // Requesting the first clue
                        for (Clue clue : clues) {
                            if (!clue.isDiscovered()) { // Find the first undiscovered clue
                                nextClue = clue;
                                break;
                            }
                        }
                    } else { // Requesting clue after a specific one was collected
                        int lastSequence = -1;
                        for(Clue c : clues) { // find sequence of last collected clue
                            if(c.getId() == lastCollectedClueId) {
                                lastSequence = c.getSequenceNumber();
                                break;
                            }
                        }

                        if (lastSequence != -1) {
                            for (Clue clue : clues) {
                                if (clue.getSequenceNumber() > lastSequence && !clue.isDiscovered()) {
                                    nextClue = clue; // Found the next undiscovered clue in sequence
                                    break;
                                }
                            }
                        }
                    }
                }
                mainThreadHandler.post(() -> callback.onComplete(nextClue));
            } catch (Exception e) {
                Log.e("QuestManager", "Error getting next clue for quest ID: " + questId, e);
                mainThreadHandler.post(() -> callback.onComplete(null)); // Or trigger onError
            }
        });
    }

    // Ensure other methods like abandonQuest, getActiveQuests, etc., are preserved from your original file.
    // For example, if you had:
    // public List<Quest> getActiveQuests() { return questRepository.getActiveQuests(); }
    // public void abandonQuest(long questId) { questRepository.delete(questId); }
    // Make sure they are still here.

    public void getCluesForQuest(long questId, QuestManagerCallback<List<Clue>> callback) {
        executorService.execute(() -> {
            try {
                List<Clue> clues = clueRepository.getCluesByQuestIdNonLiveData(questId); // Uses existing sync DAO call
                mainThreadHandler.post(() -> callback.onComplete(clues));
            } catch (Exception e) {
                Log.e("QuestManager", "Error getting all clues for quest ID: " + questId, e);
                mainThreadHandler.post(() -> callback.onComplete(new ArrayList<>())); // Return empty list on error
            }
        });
    }
}