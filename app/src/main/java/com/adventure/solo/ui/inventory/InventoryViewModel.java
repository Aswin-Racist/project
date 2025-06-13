package com.adventure.solo.ui.inventory;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.Transformations;

import com.adventure.solo.model.PlayerProfile;
import com.adventure.solo.model.Quest;
import com.adventure.solo.model.QuestProgress;
import com.adventure.solo.model.QuestStatus;
import com.adventure.solo.model.wrapper.QuestWithProgress;
import com.adventure.solo.repository.PlayerProfileRepository;
import com.adventure.solo.repository.QuestProgressRepository;
import com.adventure.solo.repository.QuestRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map; // For potential future use with Quest details map
import java.util.function.Function; // For mapping if using Java 8 stream/map
import java.util.stream.Collectors;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import android.util.Log;

@HiltViewModel
public class InventoryViewModel extends ViewModel {
    private static final String TAG = "InventoryViewModel";

    private final PlayerProfileRepository playerProfileRepository;
    private final QuestProgressRepository questProgressRepository;
    private final QuestRepository questRepository;
    private final FirebaseAuth firebaseAuth;

    private final MutableLiveData<String> _currentUserIdTrigger = new MutableLiveData<>();
    private final LiveData<PlayerProfile> _currentPlayerProfile;

    // This LiveData will be the main source for the UI, populated by combining other LiveData/data sources
    private final MutableLiveData<List<QuestWithProgress>> _completedQuestsWithDetails = new MutableLiveData<>(new ArrayList<>());
    public LiveData<List<QuestWithProgress>> completedQuestsWithDetails = _completedQuestsWithDetails;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    @Inject
    public InventoryViewModel(PlayerProfileRepository playerProfileRepository,
                                QuestProgressRepository questProgressRepository,
                                QuestRepository questRepository,
                                FirebaseAuth firebaseAuth) {
        this.playerProfileRepository = playerProfileRepository;
        this.questProgressRepository = questProgressRepository;
        this.questRepository = questRepository;
        this.firebaseAuth = firebaseAuth;

        _currentPlayerProfile = Transformations.switchMap(_currentUserIdTrigger, uid -> {
            MutableLiveData<PlayerProfile> profileData = new MutableLiveData<>();
            if (uid != null && !uid.isEmpty()) {
                playerProfileRepository.getPlayerProfile(uid, profileData::postValue);
            } else {
                profileData.postValue(null);
            }
            return profileData;
        });

        // Chain loading of completed quests based on player profile (and thus teamId)
        LiveData<List<QuestProgress>> teamQuestProgress = Transformations.switchMap(_currentPlayerProfile, profile -> {
            MutableLiveData<List<QuestProgress>> progressData = new MutableLiveData<>();
            if (profile != null && profile.teamId != null && !profile.teamId.isEmpty()) {
                _isLoading.setValue(true);
                questProgressRepository.getProgressForTeam(profile.teamId, new QuestProgressRepository.QuestProgressCallback<List<QuestProgress>>() {
                    @Override
                    public void onComplete(List<QuestProgress> result) {
                        progressData.postValue(result);
                    }
                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error fetching team quest progress for team: " + profile.teamId, e);
                        progressData.postValue(new ArrayList<>()); // Post empty on error
                         _isLoading.postValue(false);
                    }
                });
            } else {
                progressData.postValue(new ArrayList<>()); // No profile or teamId, no progress
                 _isLoading.postValue(false);
            }
            return progressData;
        });

        // Combine QuestProgress with Quest details
        // This part assumes questRepository.getAllQuests returns LiveData or we fetch all quests once
        // For simplicity with callback based repo:
        teamQuestProgress.observeForever(progressList -> {
            if (progressList == null || progressList.isEmpty()) {
                _completedQuestsWithDetails.postValue(new ArrayList<>());
                _isLoading.postValue(false);
                return;
            }

            List<QuestProgress> completedOnlyProgress = new ArrayList<>();
            for (QuestProgress qp : progressList) {
                if (qp.status == QuestStatus.COMPLETED) {
                    completedOnlyProgress.add(qp);
                }
            }

            if (completedOnlyProgress.isEmpty()) {
                _completedQuestsWithDetails.postValue(new ArrayList<>());
                _isLoading.postValue(false);
                return;
            }

            // Fetch all quests to map details. This is inefficient if there are many quests.
            // A more optimized approach would be to fetch only the quests present in completedOnlyProgress.
            // Or QuestRepository.getQuestsByIds(List<Long> ids, ...)
            questRepository.getAllQuests(new QuestRepository.QuestRepoCallback<List<Quest>>() { // Assuming this async method exists
                @Override
                public void onComplete(List<Quest> allQuests) {
                    if (allQuests == null || allQuests.isEmpty()) {
                        _completedQuestsWithDetails.postValue(new ArrayList<>());
                        _isLoading.postValue(false);
                        return;
                    }
                    List<QuestWithProgress> result = new ArrayList<>();
                    Map<Long, Quest> questMap = new HashMap<>();
                    for(Quest q : allQuests) questMap.put(q.getId(), q);

                    for (QuestProgress qp : completedOnlyProgress) {
                        Quest questDetails = questMap.get(qp.questId); // qp.questId is long
                        if (questDetails != null) {
                            result.add(new QuestWithProgress(questDetails, qp));
                        } else {
                             Log.w(TAG, "No quest details found for completed quest progress: " + qp.questId);
                        }
                    }
                    _completedQuestsWithDetails.postValue(result);
                    _isLoading.postValue(false);
                }
                 @Override
                public void onError(Exception e){ // From QuestRepository.getAllQuests
                    Log.e(TAG, "Error fetching all quests for inventory.", e);
                    _completedQuestsWithDetails.postValue(new ArrayList<>());
                    _isLoading.postValue(false);
                }
            });
        });

        refreshUser();
    }

    public void refreshUser() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        String currentUid = _currentUserIdTrigger.getValue();
        String newUid = (currentUser != null) ? currentUser.getUid() : null;

        if (newUid == null && currentUid != null) { // User logged out
             _currentUserIdTrigger.setValue(null);
        } else if (newUid != null && !newUid.equals(currentUid)) { // New user or first load
            _currentUserIdTrigger.setValue(newUid);
        } else if (newUid != null) { // Same user, force refresh if needed by re-posting same UID
             _currentUserIdTrigger.setValue(newUid);
        }
        // If newUid is null and currentUid is also null, no change, do nothing.
        Log.d(TAG, "refreshUser: Triggered with UID: " + newUid);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up observers if any were manually added with observeForever on non-Lifecycle sources.
        // LiveData observed by Fragment with getViewLifecycleOwner() are auto-cleaned.
        // The teamQuestProgress.observeForever should be removed if this ViewModel is cleared.
        // However, standard LiveData composition with switchMap handles this.
        // For now, assuming HiltViewModel and LiveData Transform.switchMap handle lifecycles.
    }
}
