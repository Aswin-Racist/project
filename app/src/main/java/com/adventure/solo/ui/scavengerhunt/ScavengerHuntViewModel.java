package com.adventure.solo.ui.scavengerhunt;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.Transformations;

import com.adventure.solo.model.PlayerProfile;
import com.adventure.solo.model.wrapper.ClueWithProgress;
import com.adventure.solo.model.wrapper.QuestWithProgress;
import com.adventure.solo.model.QuestStatus;
import com.adventure.solo.repository.PlayerProfileRepository;
import com.adventure.solo.service.QuestManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import android.util.Log;

@HiltViewModel
public class ScavengerHuntViewModel extends ViewModel {
    private static final String TAG = "ScavengerHuntVM";

    private final QuestManager questManager;
    private final PlayerProfileRepository playerProfileRepository;
    private final FirebaseAuth firebaseAuth;

    // Triggers profile loading and subsequent team-based quest loading
    private final MutableLiveData<String> _currentUserIdTrigger = new MutableLiveData<>();
    public final LiveData<PlayerProfile> currentPlayerProfile;

    private final MutableLiveData<String> _currentMissionText = new MutableLiveData<>("Login and join a team to start quests.");
    public LiveData<String> currentMissionText = _currentMissionText;

    private final MutableLiveData<QuestWithProgress> _activeQuestWithProgress = new MutableLiveData<>();
    public LiveData<QuestWithProgress> activeQuestWithProgress = _activeQuestWithProgress;

    private final MutableLiveData<List<ClueWithProgress>> _currentQuestCluesWithProgress = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<ClueWithProgress>> currentQuestCluesWithProgress = _currentQuestCluesWithProgress;

    private static final int STAMINA_COST_SEARCH = 10;
    private static final int COINS_GAINED_SEARCH = 5;

    @Inject
    public ScavengerHuntViewModel(QuestManager questManager, PlayerProfileRepository playerProfileRepository, FirebaseAuth firebaseAuth) {
        this.questManager = questManager;
        this.playerProfileRepository = playerProfileRepository;
        this.firebaseAuth = firebaseAuth;

        currentPlayerProfile = Transformations.switchMap(_currentUserIdTrigger, uid -> {
            MutableLiveData<PlayerProfile> profileData = new MutableLiveData<>();
            if (uid != null && !uid.isEmpty()) {
                Log.d(TAG, "currentPlayerProfile: Fetching profile for UID: " + uid);
                playerProfileRepository.getPlayerProfile(uid, profileData::postValue);
            } else {
                Log.d(TAG, "currentPlayerProfile: UID is null, posting null profile.");
                profileData.postValue(null);
            }
            return profileData;
        });

        // Initial trigger, especially if user is already logged in when ViewModel is created.
        refreshUserProfile();
    }

    public void refreshUserProfile() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        String uid = (currentUser != null) ? currentUser.getUid() : null;
        // PostValue if on background thread, setValue if on main. Assuming VM methods are called from main.
        if (_currentUserIdTrigger.getValue() == null && uid != null) {
            _currentUserIdTrigger.setValue(uid);
        } else if (_currentUserIdTrigger.getValue() != null && !_currentUserIdTrigger.getValue().equals(uid)) {
             _currentUserIdTrigger.setValue(uid); // User changed or logged out
        } else if (uid != null) {
            // User is the same, but we might want to force a refresh of data anyway
            _currentUserIdTrigger.setValue(uid); // Force re-trigger if already set to same UID
        } else {
             _currentUserIdTrigger.setValue(null); // No user
        }
        Log.d(TAG, "refreshUserProfile called. CurrentUser UID: " + uid);
    }

    public void loadActiveQuestForTeam() {
        PlayerProfile profile = currentPlayerProfile.getValue();
        if (profile == null || profile.teamId == null || profile.teamId.isEmpty()) {
            _currentMissionText.setValue("Join a team to start quests!");
            _activeQuestWithProgress.setValue(null);
            _currentQuestCluesWithProgress.setValue(new ArrayList<>());
            Log.d(TAG, "loadActiveQuestForTeam: Profile or teamId missing.");
            return;
        }
        String teamId = profile.teamId;
        Log.d(TAG, "loadActiveQuestForTeam: Loading for teamId: " + teamId);
        questManager.getActiveQuestsForTeam(teamId, new QuestManager.QuestManagerCallback<List<QuestWithProgress>>() {
            @Override
            public void onComplete(List<QuestWithProgress> result) {
                if (result != null && !result.isEmpty()) {
                    // TODO: Logic to select which quest if multiple are active/not_started. For now, pick first.
                    Log.d(TAG, "loadActiveQuestForTeam: Found " + result.size() + " active/not-started quests. Selecting first.");
                    setActiveQuestForTeam(result.get(0));
                } else {
                    Log.d(TAG, "loadActiveQuestForTeam: No active quests for team " + teamId);
                    _currentMissionText.setValue("No active quests for your team. Stay tuned!");
                    _activeQuestWithProgress.setValue(null);
                    _currentQuestCluesWithProgress.setValue(new ArrayList<>());
                }
            }
            @Override
            public void onError(Exception e) {
                _currentMissionText.setValue("Error loading team quests.");
                Log.e(TAG, "Error loading team quests for team " + teamId, e);
            }
        });
    }

    public void setActiveQuestForTeam(QuestWithProgress qwp) {
        _activeQuestWithProgress.setValue(qwp);
        if (qwp != null && qwp.quest != null) {
            Log.d(TAG, "setActiveQuestForTeam: Set to " + qwp.quest.getTitle());
            _currentMissionText.setValue("Current Quest: " + qwp.quest.getTitle());
            loadCluesForActiveTeamQuest(qwp.quest.getId());
        } else {
             Log.d(TAG, "setActiveQuestForTeam: QuestWithProgress is null, clearing active quest.");
             _currentMissionText.setValue("No active team quest selected.");
             _currentQuestCluesWithProgress.setValue(new ArrayList<>());
        }
    }

    private void loadCluesForActiveTeamQuest(long questId) {
        PlayerProfile profile = currentPlayerProfile.getValue();
        if (profile == null || profile.teamId == null) {
            Log.e(TAG, "loadCluesForActiveTeamQuest: Profile or teamId is null. Cannot load clues.");
            _currentQuestCluesWithProgress.setValue(new ArrayList<>());
            _currentMissionText.setValue("Error: Player or team data missing for loading clues.");
            return;
        }
        String teamId = profile.teamId;
        Log.d(TAG, "loadCluesForActiveTeamQuest: Loading clues for quest " + questId + ", team " + teamId);
        questManager.getCluesForTeamQuest(questId, teamId, new QuestManager.QuestManagerCallback<List<ClueWithProgress>>() {
            @Override
            public void onComplete(List<ClueWithProgress> result) {
                _currentQuestCluesWithProgress.setValue(result != null ? result : new ArrayList<>());
                updateMissionTextFromClues(result);
            }
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error loading clues for team quest " + questId, e);
                 _currentQuestCluesWithProgress.setValue(new ArrayList<>());
                 _currentMissionText.setValue("Error loading clues for current quest.");
            }
        });
    }

    private void updateMissionTextFromClues(List<ClueWithProgress> clues) {
        QuestWithProgress currentQwp = _activeQuestWithProgress.getValue();
        if (currentQwp == null || currentQwp.quest == null) {
             _currentMissionText.setValue("No active team mission.");
             Log.d(TAG, "updateMissionTextFromClues: No active quest, mission text set to default.");
             return;
        }

        // Check if the quest itself is completed (this status should be updated by clueCollected logic)
        if (currentQwp.progress != null && currentQwp.progress.status == QuestStatus.COMPLETED) {
            _currentMissionText.setValue("Quest '" + currentQwp.quest.getTitle() + "' Complete!");
            Log.i(TAG, "updateMissionTextFromClues: Quest '" + currentQwp.quest.getTitle() + "' is COMPLETED.");
            return;
        }

        if (clues == null || clues.isEmpty()) {
            Log.d(TAG, "updateMissionTextFromClues: Clues list is null or empty for quest " + currentQwp.quest.getTitle() + ". Checking if quest should be complete.");
            // This condition implies all clues might have been discovered, or there were no clues.
            // Re-fetch quest progress to ensure status is up-to-date.
            PlayerProfile profile = currentPlayerProfile.getValue();
            if (profile != null && profile.teamId != null) {
                // This nested call could be complex. Simpler: assume QuestManager updates QuestProgress,
                // and we just need to reflect that the current QWP's progress object is up-to-date.
                // For now, if no clues are passed, and quest not marked complete, assume it's pending or error.
                 _currentMissionText.setValue("All clues found for '" + currentQwp.quest.getTitle() + "'! Verifying quest completion...");
                 // Trigger a refresh of the quest state if needed, though clueCollected should handle this.
                 // loadActiveQuestForTeam(); // This might be too broad, could re-fetch just this quest's progress.
            }
            return;
        }

        ClueWithProgress firstUnsolved = null;
        for (ClueWithProgress cwp : clues) {
            if (cwp.clue != null && (cwp.progress == null || !cwp.progress.discoveredByTeam)) {
                firstUnsolved = cwp;
                break;
            }
        }

        if (firstUnsolved != null && firstUnsolved.clue != null) {
            _currentMissionText.setValue("Next Clue: " + firstUnsolved.clue.getText());
            Log.d(TAG, "updateMissionTextFromClues: Next clue is '" + firstUnsolved.clue.getText() + "'");
        } else {
            _currentMissionText.setValue("All clues for '" + currentQwp.quest.getTitle() + "' found! Quest should be complete.");
            Log.d(TAG, "updateMissionTextFromClues: All clues found for " + currentQwp.quest.getTitle());
            // To be very sure, one might re-fetch the quest progress here if not updated by other flows.
            // For example, by calling a method in QuestManager to get specific QuestWithProgress.
            // questManager.getQuestDetailsForTeam(currentQwp.quest.getId(), profile.teamId, updatedQwp -> { ... });
            // This is to ensure the QuestProgress object within currentQwp is the latest.
            // However, clueCollected should ideally lead to QuestManager updating the QuestProgress,
            // and then refreshing the activeQuestWithProgress LiveData.
        }
    }

    public void clueCollectedByPlayer(long collectedClueId, int rewardPoints) {
        FirebaseUser fbUser = firebaseAuth.getCurrentUser();
        PlayerProfile profile = currentPlayerProfile.getValue();
        QuestWithProgress qwp = _activeQuestWithProgress.getValue();

        if (fbUser == null || profile == null || profile.teamId == null || qwp == null || qwp.quest == null) {
            Log.e(TAG, "clueCollectedByPlayer: Pre-conditions not met (user, profile, teamId, or active quest missing).");
            _currentMissionText.setValue("Error processing clue collection (missing critical data).");
            return;
        }

        String playerId = fbUser.getUid();
        String teamId = profile.teamId;
        long questId = qwp.quest.getId();
        Log.d(TAG, "clueCollectedByPlayer: Clue " + collectedClueId + " by player " + playerId + " for team " + teamId + " in quest " + questId);

        // Award XP immediately locally, then sync with DB
        playerProfileRepository.addExperience(playerId, rewardPoints, success -> {
            if (success) Log.d(TAG, "XP ("+rewardPoints+") awarded to " + playerId + " in DB.");
            else Log.e(TAG, "Failed to award XP to " + playerId + " in DB.");
            refreshUserProfile(); // Refresh profile to get updated XP and other stats (like coins if quest completes)
        });

        questManager.markClueAsDiscovered(collectedClueId, playerId, teamId, new QuestManager.QuestManagerCallback<Boolean>() {
            @Override
            public void onComplete(Boolean result) {
                if (result) {
                    Log.d(TAG, "Clue " + collectedClueId + " successfully marked as discovered for team " + teamId + " by QuestManager.");
                    // After marking discovered, QuestManager's checkAndCompleteQuestForTeam runs.
                    // That method, if quest completes, calls awardTeamCompletionBonus which updates player coins.
                    // So, we need to refresh the active quest (to get its new completed status)
                    // and its clues (to get updated discovered statuses for all clues).
                    loadActiveQuestForTeam(); // This will re-evaluate active quest and its clues.
                } else {
                    Log.e(TAG, "QuestManager reported failure to mark clue " + collectedClueId + " as discovered.");
                    _currentMissionText.setValue("Failed to update clue discovery status in DB.");
                }
            }
            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Error marking clue discovered via QuestManager", e);
                _currentMissionText.setValue("Error updating clue: " + e.getMessage());
            }
        });
    }

    public void performSearch() {
        FirebaseUser fbUser = firebaseAuth.getCurrentUser();
        PlayerProfile profile = currentPlayerProfile.getValue();
        if (fbUser == null || profile == null) {
             _currentMissionText.setValue("Login to perform actions.");
             Log.w(TAG, "performSearch: User or profile is null.");
             return;
        }

        if (profile.stamina >= STAMINA_COST_SEARCH) {
            int newStamina = profile.stamina - STAMINA_COST_SEARCH;
            int newCoins = profile.coins + COINS_GAINED_SEARCH;

            // This method needs to be added to PlayerProfileRepository
            playerProfileRepository.updateStaminaAndCoins(fbUser.getUid(), newStamina, newCoins, success -> {
                if(success) {
                    Log.d(TAG, "Search successful. Stamina/Coins updated in DB for UID: " + fbUser.getUid());
                    refreshUserProfile(); // Refresh profile LiveData to show new stamina/coins
                } else {
                    Log.e(TAG, "Search: Failed to update stamina/coins in DB for UID: " + fbUser.getUid());
                     _currentMissionText.setValue("Search action failed, please try again.");
                     refreshUserProfile(); // Refresh to show actual DB values if update failed
                }
            });
        } else {
             _currentMissionText.setValue("Not enough stamina to search! Need " + STAMINA_COST_SEARCH + ", have " + profile.stamina);
             Log.d(TAG, "performSearch: Not enough stamina. Have " + profile.stamina + ", Need " + STAMINA_COST_SEARCH);
        }
    }
}
