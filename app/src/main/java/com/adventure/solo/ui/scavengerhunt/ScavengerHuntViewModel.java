package com.adventure.solo.ui.scavengerhunt;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.adventure.solo.model.Clue;
import com.adventure.solo.model.Quest;
import com.adventure.solo.service.QuestManager;

import java.util.ArrayList; // Added import
import java.util.Collections; // Added import for Collections.sort if needed later
import java.util.Comparator;  // Added import for Comparator if needed later
import java.util.List;       // Added import

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import android.util.Log;


@HiltViewModel
public class ScavengerHuntViewModel extends ViewModel {
    private static final String TAG = "ScavengerHuntVM";

    private final QuestManager questManager;

    private final MutableLiveData<Integer> playerScore = new MutableLiveData<>(0);
    private final MutableLiveData<String> currentMissionText = new MutableLiveData<>("No active mission.");
    private final MutableLiveData<Quest> activeQuest = new MutableLiveData<>();
    private final MutableLiveData<Clue> nextClue = new MutableLiveData<>();
    private final MutableLiveData<List<Clue>> currentQuestClues = new MutableLiveData<>();

    // New LiveData for Stamina and Coins
    private final MutableLiveData<Integer> playerStamina = new MutableLiveData<>(100); // Default 100
    private final MutableLiveData<Integer> playerCoins = new MutableLiveData<>(0);    // Default 0

    private static final int STAMINA_COST_SEARCH = 10;
    private static final int COINS_GAINED_SEARCH = 5; // Example: Gain 5 coins on successful search

    @Inject
    public ScavengerHuntViewModel(QuestManager questManager) {
        this.questManager = questManager;
        Log.d(TAG, "ScavengerHuntViewModel created with QuestManager: " + questManager);
    }

    public LiveData<Integer> getPlayerScore() { return playerScore; }
    public LiveData<String> getCurrentMissionText() { return currentMissionText; }
    public LiveData<Quest> getActiveQuest() { return activeQuest; }
    public LiveData<Clue> getNextClue() { return nextClue; }
    public LiveData<List<Clue>> getCurrentQuestClues() { return currentQuestClues; }
    public LiveData<Integer> getPlayerStamina() { return playerStamina; } // Getter for Stamina
    public LiveData<Integer> getPlayerCoins() { return playerCoins; }     // Getter for Coins

    public void addScore(int points) {
        Integer currentScore = playerScore.getValue();
        playerScore.setValue((currentScore != null ? currentScore : 0) + points);
        Log.d(TAG, "Score updated to: " + playerScore.getValue());
    }

    public void setActiveQuest(Quest quest) {
        activeQuest.setValue(quest);
        if (quest != null) {
            updateMissionTextForNewQuest(quest);
            loadCluesForQuest(quest); // This will load all clues and find the first one for mission text
        } else {
            updateMissionTextForNewQuest(null);
            currentQuestClues.postValue(new ArrayList<>()); // Clear clues
            nextClue.postValue(null); // Clear next specific clue
        }
    }

    private void loadCluesForQuest(Quest quest) {
        if (quest == null) {
            currentQuestClues.postValue(new ArrayList<>());
            updateMissionTextWithClue(null); // No quest, no first clue
            return;
        }
        questManager.getCluesForQuest(quest.getId(), clues -> { // This is the new method in QuestManager
            currentQuestClues.postValue(clues); // Update LiveData for map markers
            if (clues != null && !clues.isEmpty()) {
                // Find the first unsolved clue to set as the 'nextClue' and update mission text
                Clue firstUnsolvedClue = null;
                // Clues should be sorted by sequence from DAO/QuestManager
                for (Clue c : clues) {
                    if (!c.isDiscovered()) {
                        firstUnsolvedClue = c;
                        break;
                    }
                }
                updateMissionTextWithClue(firstUnsolvedClue); // Sets nextClue LiveData too
            } else {
                // No clues for this quest, or all are solved
                updateMissionTextWithClue(null);
            }
        });
    }

    public void clueCollected(long collectedClueId, Quest currentQuestFromFragment) {
        if (currentQuestFromFragment == null) {
            Log.e(TAG, "clueCollected called with null currentQuestFromFragment.");
            currentMissionText.setValue("Error: Quest context lost.");
            return;
        }
        Log.d(TAG, "Processing collection for Clue ID: " + collectedClueId + " of Quest ID: " + currentQuestFromFragment.getId());

        questManager.getQuestDetails(currentQuestFromFragment.getId(), updatedQuest -> {
            if (updatedQuest != null) {
                activeQuest.setValue(updatedQuest);

                questManager.getCluesForQuest(updatedQuest.getId(), updatedClues -> {
                    currentQuestClues.postValue(updatedClues);

                    if (updatedQuest.isCompleted()) {
                        Log.i(TAG, "Quest " + updatedQuest.getId() + " is now complete.");
                        updateMissionTextWithClue(null);
                    } else {
                        Log.d(TAG, "Quest " + updatedQuest.getId() + " not complete, fetching next specific clue after " + collectedClueId);
                        questManager.getNextClueForQuest(updatedQuest.getId(), collectedClueId, specificNextClue -> {
                            updateMissionTextWithClue(specificNextClue);
                        });
                    }
                });
            } else {
                Log.e(TAG, "Failed to get updated quest details for quest ID: " + currentQuestFromFragment.getId());
                currentMissionText.setValue("Error retrieving quest status post-collection.");
            }
        });
    }

    // This method updates both the mission text and the 'nextClue' LiveData
    public void updateMissionTextWithClue(Clue clueForMission) {
        nextClue.setValue(clueForMission); // Update the specific next clue
        if (clueForMission != null) {
            currentMissionText.setValue("Next Clue: " + clueForMission.getText());
            Log.d(TAG, "Mission text updated for next clue: " + clueForMission.getId() + " - " + clueForMission.getText());
        } else {
            Quest currentQ = activeQuest.getValue();
            if (currentQ != null && currentQ.isCompleted()) {
                 currentMissionText.setValue("Quest '" + currentQ.getTitle() + "' Complete!");
                 Log.i(TAG, "Mission text updated: Quest '" + currentQ.getTitle() + "' is complete.");
            } else if (currentQ != null) {
                // All clues might be discovered, but quest isn't marked complete yet, or no clues found.
                currentMissionText.setValue("All clues found for '" + currentQ.getTitle() + "' or no more clues available.");
                Log.w(TAG, "No next clue for '" + currentQ.getTitle() + "', but quest not marked complete in ViewModel, or no clues.");
            } else {
                currentMissionText.setValue("No active mission.");
                Log.d(TAG, "Mission text updated: No active mission.");
            }
        }
    }

    public void updateMissionTextForNewQuest(Quest quest) {
        if (quest != null) {
            currentMissionText.setValue("New Quest: " + quest.getTitle() + "\n" + quest.getDescription());
            Log.d(TAG, "Mission text updated for new quest: " + quest.getTitle());
        } else {
            currentMissionText.setValue("No active mission.");
            Log.d(TAG, "Mission text updated: No active mission (new quest was null).");
        }
    }

    public void setMissionText(String text) {
        currentMissionText.setValue(text);
        Log.d(TAG, "Mission text explicitly set to: " + text);
    }

    // New method for "Search" button
    public boolean performSearch() {
        Integer currentStamina = playerStamina.getValue();
        if (currentStamina != null && currentStamina >= STAMINA_COST_SEARCH) {
            playerStamina.setValue(currentStamina - STAMINA_COST_SEARCH);

            Integer currentCoins = playerCoins.getValue();
            playerCoins.setValue((currentCoins != null ? currentCoins : 0) + COINS_GAINED_SEARCH);
            Log.d(TAG, "Search performed. Stamina: " + playerStamina.getValue() + ", Coins: " + playerCoins.getValue());
            return true; // Search successful
        } else {
            Log.d(TAG, "Search failed. Not enough stamina. Current: " + (currentStamina != null ? currentStamina : "null"));
            return false; // Not enough stamina
        }
    }
}
