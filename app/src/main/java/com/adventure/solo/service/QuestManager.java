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

// New model and repository imports for team progress
import com.adventure.solo.model.PlayerProfile;
import com.adventure.solo.model.firebase.Team;
import com.adventure.solo.model.ClueProgress;
import com.adventure.solo.model.QuestProgress;
import com.adventure.solo.model.QuestStatus;
import com.adventure.solo.model.wrapper.ClueWithProgress;
import com.adventure.solo.model.wrapper.QuestWithProgress;
import com.adventure.solo.repository.PlayerProfileRepository;
import com.adventure.solo.repository.TeamRepository;
import com.adventure.solo.repository.ClueProgressRepository;
import com.adventure.solo.repository.QuestProgressRepository;


@Singleton
public class QuestManager {
    private static final String TAG = "QuestManager"; // Added TAG
    private final QuestGenerator questGenerator;
    private final QuestRepository questRepository;
    private final ClueRepository clueRepository;
    private final PlayerProfileRepository playerProfileRepository; // Added
    private final TeamRepository teamRepository; // Added
    private final ClueProgressRepository clueProgressRepository; // Added
    private final QuestProgressRepository questProgressRepository; // Added

    private static final float PROXIMITY_RADIUS = 50;
    private static final int DEFAULT_CLUES_PER_QUEST = 5;

    private final ExecutorService executorService;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    // Constants for rewards (can be moved to a config file or class)
    private static final int XP_PER_CLUE = 10;
    private static final int COINS_QUEST_COMPLETION_BONUS = 50;


    @Inject
    public QuestManager(QuestGenerator questGenerator,
                       QuestRepository questRepository,
                       ClueRepository clueRepository,
                       PlayerProfileRepository playerProfileRepository, // Added
                       TeamRepository teamRepository, // Added
                       ClueProgressRepository clueProgressRepository, // Added
                       QuestProgressRepository questProgressRepository) { // Added
        this.questGenerator = questGenerator;
        this.questRepository = questRepository;
        this.clueRepository = clueRepository;
        this.playerProfileRepository = playerProfileRepository; // Added
        this.teamRepository = teamRepository; // Added
        this.clueProgressRepository = clueProgressRepository; // Added
        this.questProgressRepository = questProgressRepository; // Added
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public interface QuestManagerCallback<T> {
        void onComplete(T result);
        void onError(Exception e); // Added onError to callback
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

    // New team-based markClueAsDiscovered
    public void markClueAsDiscovered(long clueId, String playerId, String teamId, QuestManagerCallback<Boolean> callback) {
        executorService.execute(() -> {
            try {
                Clue clue = clueRepository.getClueById(clueId);
                if (clue == null) {
                    throw new Exception("Clue not found with id: " + clueId);
                }
                long questIdForClue = clue.getQuestId();

                clueProgressRepository.getClueProgress(clueId, teamId, new ClueProgressRepository.ClueProgressCallback<ClueProgress>() {
                    @Override
                    public void onComplete(ClueProgress existingProgress) {
                        try {
                            ClueProgress progressToUpdate = existingProgress;
                            if (progressToUpdate == null) {
                                progressToUpdate = new ClueProgress(clueId, teamId, questIdForClue);
                            }

                            if (!progressToUpdate.isDiscoveredByTeam()) {
                                progressToUpdate.setDiscoveredByTeam(true);
                                progressToUpdate.setDiscoveredByPlayerId(playerId);

                                final ClueProgress finalProgressToUpdate = progressToUpdate;
                                clueProgressRepository.insertOrUpdate(finalProgressToUpdate, new ClueProgressRepository.ClueProgressCallback<Void>() {
                                    @Override
                                    public void onComplete(Void result) {
                                        playerProfileRepository.addExperience(playerId, XP_PER_CLUE, xpSuccess -> {
                                            if (!xpSuccess) Log.e(TAG, "Failed to add XP for player " + playerId + " for clue " + clueId);
                                        });
                                        // Pass the original callback to the completion check
                                        checkAndCompleteQuestForTeam(questIdForClue, teamId, playerId, callback);
                                    }
                                    @Override
                                    public void onError(Exception e) { // onError for insertOrUpdate
                                        Log.e(TAG, "Failed to update clue progress for clueId " + clueId + " teamId " + teamId, e);
                                        mainThreadHandler.post(() -> callback.onError(e));
                                    }
                                });
                            } else {
                                mainThreadHandler.post(() -> callback.onComplete(true)); // Already discovered, still a "success" for this call
                            }
                        } catch (Exception e) {
                             Log.e(TAG, "Inner error in markClueAsDiscovered (team): " + e.getMessage(), e);
                             mainThreadHandler.post(() -> callback.onError(e));
                        }
                    }
                     @Override
                    public void onError(Exception e) { // onError for getClueProgress
                        Log.e(TAG, "Failed to get existing clue progress for clueId " + clueId + " teamId " + teamId, e);
                        mainThreadHandler.post(() -> callback.onError(e));
                    }
                });
            } catch (Exception e) { // Catch error from clueRepository.getClueById or other initial sync errors
                Log.e(TAG, "Outer error in markClueAsDiscovered (team): " + e.getMessage(), e);
                mainThreadHandler.post(() -> callback.onError(e));
            }
        });
    }

    private void checkAndCompleteQuestForTeam(long questId, String teamId, String discoveringPlayerId, QuestManagerCallback<Boolean> originalCallback) {
        questProgressRepository.getQuestProgress(questId, teamId, new QuestProgressRepository.QuestProgressCallback<QuestProgress>() {
            @Override
            public void onComplete(QuestProgress questProgress) {
                 try {
                    if (questProgress != null && questProgress.getStatus() == QuestStatus.COMPLETED) {
                        Log.d(TAG, "Quest " + questId + " already completed for team " + teamId);
                        if(originalCallback != null) mainThreadHandler.post(() -> originalCallback.onComplete(true));
                        return;
                    }

                    clueProgressRepository.getProgressForQuestByTeam(questId, teamId, new ClueProgressRepository.ClueProgressCallback<List<ClueProgress>>() {
                        @Override
                        public void onComplete(List<ClueProgress> clueProgressList) {
                            try {
                                List<Clue> allCluesForQuest = clueRepository.getCluesByQuestIdNonLiveData(questId);
                                if (allCluesForQuest == null || allCluesForQuest.isEmpty()) {
                                    Log.w(TAG, "No clues found for quest " + questId + ". Cannot determine completion for team " + teamId);
                                    if(originalCallback != null) mainThreadHandler.post(() -> originalCallback.onComplete(true)); // Clue discovery was success
                                    return;
                                }

                                boolean allDiscovered = true;
                                if (clueProgressList == null || clueProgressList.size() < allCluesForQuest.size()) {
                                    allDiscovered = false;
                                } else {
                                    for (Clue clue : allCluesForQuest) {
                                        boolean foundProgressForThisClue = false;
                                        for (ClueProgress cp : clueProgressList) {
                                            if (cp.actualClueId == clue.getId() && cp.isDiscoveredByTeam()) {
                                                foundProgressForThisClue = true;
                                                break;
                                            }
                                        }
                                        if (!foundProgressForThisClue) {
                                            allDiscovered = false;
                                            break;
                                        }
                                    }
                                }

                                if (allDiscovered) {
                                    QuestProgress currentQp = questProgress != null ? questProgress : new QuestProgress(questId, teamId);
                                    currentQp.setStatus(QuestStatus.COMPLETED);
                                    currentQp.setLastCompletedByPlayerId(discoveringPlayerId);

                                    questProgressRepository.insertOrUpdate(currentQp, new QuestProgressRepository.QuestProgressCallback<Void>() {
                                        @Override public void onComplete(Void result) {
                                            Log.i(TAG, "Quest " + questId + " marked COMPLETED for team " + teamId);
                                            awardTeamCompletionBonus(teamId);
                                            if(originalCallback != null) mainThreadHandler.post(() -> originalCallback.onComplete(true));
                                        }
                                        @Override public void onError(Exception e) {
                                            Log.e(TAG, "Failed to update QP to COMPLETED for Q:" + questId + " T:" + teamId, e);
                                            if(originalCallback != null) mainThreadHandler.post(() -> originalCallback.onError(e));
                                        }
                                    });
                                } else {
                                    Log.d(TAG, "Quest " + questId + " not yet complete for team " + teamId);
                                    if(originalCallback != null) mainThreadHandler.post(() -> originalCallback.onComplete(true));
                                }
                            } catch (Exception e) { // Catch for errors inside clueProgressRepository.getProgressForQuestByTeam callback
                                Log.e(TAG, "Error processing clue progresses for quest completion check", e);
                                if(originalCallback != null) mainThreadHandler.post(() -> originalCallback.onError(e));
                            }
                        }
                        @Override public void onError(Exception e) { // onError for clueProgressRepository.getProgressForQuestByTeam
                             Log.e(TAG, "Failed to get clue progresses for quest completion check", e);
                             if(originalCallback != null) mainThreadHandler.post(() -> originalCallback.onError(e));
                        }
                    });
                 } catch (Exception e) { // Catch for errors inside questProgressRepository.getQuestProgress callback
                    Log.e(TAG, "Error processing quest progress for quest completion check", e);
                    if(originalCallback != null) mainThreadHandler.post(() -> originalCallback.onError(e));
                 }
            }
            @Override public void onError(Exception e) { // onError for questProgressRepository.getQuestProgress
                Log.e(TAG, "Failed to get quest progress for quest completion check", e);
                if(originalCallback != null) mainThreadHandler.post(() -> originalCallback.onError(e));
            }
        });
    }

    private void awardTeamCompletionBonus(String teamId) {
        teamRepository.getTeam(teamId, new TeamRepository.TeamDataCallback() {
            @Override
            public void onComplete(Team team) {
                if (team != null && team.memberPlayerIds != null) {
                    for (String memberId : team.memberPlayerIds) {
                        playerProfileRepository.addCoins(memberId, COINS_QUEST_COMPLETION_BONUS, coinSuccess -> {
                            if (!coinSuccess) Log.e(TAG, "Failed to add completion bonus coins to player " + memberId);
                        });
                    }
                }
            }
            // TeamRepository.TeamDataCallback from previous step does not have onError.
            // If it did, it would be:
            // @Override public void onError(Exception e) { Log.e(TAG, "Failed to get team for bonus award: " + e.getMessage()); }
        });
    }


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

    public void getActiveQuestsForTeam(String teamId, QuestManagerCallback<List<QuestWithProgress>> callback) {
        executorService.execute(() -> { // Still run overall logic on executor
            try {
                List<Quest> allQuests = questRepository.getAllQuestsNonLiveData();
                if (allQuests == null) allQuests = new ArrayList<>();

                // Use QuestProgressRepository for async fetch
                questProgressRepository.getProgressForTeam(teamId, new QuestProgressRepository.QuestProgressCallback<List<QuestProgress>>() {
                    @Override
                    public void onComplete(List<QuestProgress> teamProgresses) {
                        try {
                            List<QuestWithProgress> result = new ArrayList<>();
                            for (Quest quest : allQuests) { // allQuests is from background thread, safe here
                                QuestProgress progress = null;
                                if (teamProgresses != null) {
                                    for (QuestProgress tp : teamProgresses) {
                                        if (tp.questId == quest.getId()) {
                                            progress = tp;
                                            break;
                                        }
                                    }
                                }
                                if (progress == null || progress.getStatus() == QuestStatus.NOT_STARTED || progress.getStatus() == QuestStatus.IN_PROGRESS) {
                                    result.add(new QuestWithProgress(quest, progress));
                                }
                            }
                            mainThreadHandler.post(() -> callback.onComplete(result));
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing team progresses for active quests", e);
                            mainThreadHandler.post(() -> callback.onError(e));
                        }
                    }

                    @Override
                    public void onError(Exception e) { // onError from questProgressRepository.getProgressForTeam
                        Log.e(TAG, "Failed to get team progress for active quests", e);
                        mainThreadHandler.post(() -> callback.onError(e));
                    }
                });
            } catch (Exception e) { // Catch error from sync questRepository.getAllQuestsNonLiveData()
                Log.e(TAG, "Failed to get all quests for active quests for team", e);
                mainThreadHandler.post(() -> callback.onError(e));
            }
        });
    }

    public void getCluesForTeamQuest(long questId, String teamId, QuestManagerCallback<List<ClueWithProgress>> callback) {
        executorService.execute(() -> { // Still run overall logic on executor
            try {
                List<Clue> allClues = clueRepository.getCluesByQuestIdNonLiveData(questId);
                if (allClues == null) allClues = new ArrayList<>();

                // Use ClueProgressRepository for async fetch
                clueProgressRepository.getProgressForQuestByTeam(questId, teamId, new ClueProgressRepository.ClueProgressCallback<List<ClueProgress>>() {
                     @Override
                     public void onComplete(List<ClueProgress> teamClueProgresses) {
                        try {
                            List<ClueWithProgress> result = new ArrayList<>();
                            for (Clue clue : allClues) { // allClues is from background thread
                                ClueProgress progress = null;
                                if (teamClueProgresses != null) {
                                    for (ClueProgress cp : teamClueProgresses) {
                                        if (cp.actualClueId == clue.getId()) {
                                            progress = cp;
                                            break;
                                        }
                                    }
                                }
                                result.add(new ClueWithProgress(clue, progress));
                            }
                            mainThreadHandler.post(() -> callback.onComplete(result));
                        } catch (Exception e) { // Catch errors from processing lists
                            Log.e(TAG, "Error processing team clue progresses", e);
                            mainThreadHandler.post(() -> callback.onError(e));
                        }
                     }
                     @Override
                     public void onError(Exception e) { // onError for clueProgressRepository.getProgressForQuestByTeam
                        Log.e(TAG, "Failed to get team clue progresses", e);
                        mainThreadHandler.post(() -> callback.onError(e));
                     }
                });
            } catch (Exception e) { // Catch error from sync clueRepository.getCluesByQuestIdNonLiveData
                 Log.e(TAG, "Failed to get all clues for team quest", e);
                 mainThreadHandler.post(() -> callback.onError(e));
            }
        });
    }

    // TODO: Add back other QuestManager methods like generateNewQuest, getActiveQuests (original), etc.
    // if they are still needed, adapting them for team context or player context as necessary.
    // For example: existing getQuestClues(long questId) and getNextClue(long questId) are for single player.
    // They might need to be adapted or new team-aware versions created if general "next clue for team" is needed.

    public void getQuestDetailsForTeam(long questId, String teamId, QuestManagerCallback<QuestWithProgress> callback) {
        executorService.execute(() -> {
            try {
                Quest quest = questRepository.getQuestById(questId); // Assuming getQuestById is synchronous
                if (quest == null) {
                    mainThreadHandler.post(() -> callback.onError(new Exception("Quest not found: " + questId)));
                    return;
                }
                questProgressRepository.getQuestProgress(questId, teamId, new QuestProgressRepository.QuestProgressCallback<QuestProgress>() {
                    @Override
                    public void onComplete(QuestProgress progress) {
                        // progress can be null if the team hasn't started this quest yet
                        mainThreadHandler.post(() -> callback.onComplete(new QuestWithProgress(quest, progress)));
                    }
                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error fetching quest progress for Q:" + questId + " T:" + teamId, e);
                        // Still return quest details, but progress will be null or error indicated
                        mainThreadHandler.post(() -> callback.onError(e));
                    }
                });
            } catch (Exception e) { // Catch error from questRepository.getQuestById()
                Log.e(TAG, "Error fetching quest details for Q:" + questId, e);
                mainThreadHandler.post(() -> callback.onError(e));
            }
        });
    }
}