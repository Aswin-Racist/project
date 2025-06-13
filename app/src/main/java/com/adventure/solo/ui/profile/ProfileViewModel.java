package com.adventure.solo.ui.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.Transformations;

import com.adventure.solo.model.PlayerProfile;
import com.adventure.solo.model.firebase.Team;
import com.adventure.solo.repository.PlayerProfileRepository;
import com.adventure.solo.repository.TeamRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import android.util.Log;

@HiltViewModel
public class ProfileViewModel extends ViewModel {
    private static final String TAG = "ProfileViewModel";

    private final PlayerProfileRepository playerProfileRepository;
    private final TeamRepository teamRepository;
    private final FirebaseAuth firebaseAuth;

    private final MutableLiveData<String> currentUserIdTrigger = new MutableLiveData<>();
    public final LiveData<PlayerProfile> playerProfile;
    public final LiveData<Team> currentTeam;

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> toastMessage = new MutableLiveData<>();


    @Inject
    public ProfileViewModel(PlayerProfileRepository playerProfileRepository, TeamRepository teamRepository, FirebaseAuth firebaseAuth) {
        this.playerProfileRepository = playerProfileRepository;
        this.teamRepository = teamRepository;
        this.firebaseAuth = firebaseAuth;

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            currentUserIdTrigger.setValue(user.getUid());
        } else {
            Log.e(TAG, "No Firebase user logged in at ViewModel creation.");
            // Post a null profile or an error state if appropriate
        }

        playerProfile = Transformations.switchMap(currentUserIdTrigger, uid -> {
            MutableLiveData<PlayerProfile> profileData = new MutableLiveData<>();
            if (uid != null && !uid.isEmpty()) {
                playerProfileRepository.getPlayerProfile(uid, profileData::postValue);
            } else {
                profileData.postValue(null);
            }
            return profileData;
        });

        currentTeam = Transformations.switchMap(playerProfile, profile -> {
            MutableLiveData<Team> teamData = new MutableLiveData<>();
            if (profile != null && profile.teamId != null && !profile.teamId.isEmpty()) {
                teamRepository.getTeam(profile.teamId, new TeamRepository.TeamDataCallback() {
                    @Override
                    public void onComplete(Team team) {
                        teamData.postValue(team);
                    }
                });
            } else {
                teamData.postValue(null);
            }
            return teamData;
        });
    }

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getToastMessage() { return toastMessage; }
    public void clearToastMessage() { toastMessage.setValue(null); }

    // Call this to refresh data, e.g., after login or when screen is shown
    public void refreshUserProfile() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            currentUserIdTrigger.setValue(user.getUid()); // Re-trigger LiveData chain
        } else {
            currentUserIdTrigger.setValue(null); // Clear profile if user logged out
        }
    }


    public void createTeam(String teamName) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            toastMessage.setValue("User not logged in.");
            return;
        }
        if (teamName == null || teamName.trim().isEmpty()) {
            toastMessage.setValue("Team name cannot be empty.");
            return;
        }
        // Prevent creating a team if already in one
        PlayerProfile currentProf = playerProfile.getValue();
        if (currentProf != null && currentProf.teamId != null && !currentProf.teamId.isEmpty()) {
            toastMessage.setValue("You are already in a team. Leave your current team first.");
            return;
        }

        isLoading.setValue(true);
        String leaderUsername = user.getDisplayName() != null ? user.getDisplayName() : "Leader";

        teamRepository.createTeam(teamName, user.getUid(), leaderUsername,
            (success, teamIdOrError) -> {
                if (success && teamIdOrError != null) {
                    playerProfileRepository.updateTeamIdForPlayer(user.getUid(), teamIdOrError, updateSuccess -> {
                        if (updateSuccess) {
                            toastMessage.postValue("Team created! Team ID: " + teamIdOrError);
                            currentUserIdTrigger.postValue(user.getUid()); // Re-trigger to refresh profile and team
                        } else {
                            toastMessage.postValue("Team created on server, but failed to update local profile.");
                            // TODO: Handle inconsistency - maybe try to remove player from Firebase team or delete team
                        }
                        isLoading.postValue(false);
                    });
                } else {
                    toastMessage.postValue("Failed to create team: " + teamIdOrError);
                    isLoading.postValue(false);
                }
        });
    }

    public void joinTeam(String teamIdToJoin) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            toastMessage.setValue("User not logged in.");
            return;
        }
        if (teamIdToJoin == null || teamIdToJoin.trim().isEmpty()) {
            toastMessage.setValue("Team ID cannot be empty.");
            return;
        }
        PlayerProfile currentProf = playerProfile.getValue();
        if (currentProf != null && currentProf.teamId != null && !currentProf.teamId.isEmpty()) {
            toastMessage.setValue("You are already in a team. Leave your current team first.");
            return;
        }
        isLoading.setValue(true);
        String playerUsername = user.getDisplayName() != null ? user.getDisplayName() : "New Member";

        teamRepository.addPlayerToTeam(user.getUid(), playerUsername, teamIdToJoin,
            (success, message) -> {
                if (success) {
                    playerProfileRepository.updateTeamIdForPlayer(user.getUid(), teamIdToJoin, updateSuccess -> {
                        if (updateSuccess) {
                            toastMessage.postValue("Joined team successfully!");
                            currentUserIdTrigger.postValue(user.getUid());
                        } else {
                            toastMessage.postValue("Joined team on server, but failed to update local profile.");
                            // TODO: Handle inconsistency - try to remove player from Firebase team
                        }
                        isLoading.postValue(false);
                    });
                } else {
                    toastMessage.postValue("Failed to join team: " + message);
                    isLoading.postValue(false);
                }
        });
    }

    public void leaveTeam() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        PlayerProfile currentProf = playerProfile.getValue();
        if (user == null || currentProf == null || currentProf.teamId == null || currentProf.teamId.isEmpty()) {
            toastMessage.setValue("Not in a team or user not logged in.");
            return;
        }
        isLoading.setValue(true);
        String teamIdToLeave = currentProf.teamId;
        teamRepository.removePlayerFromTeam(user.getUid(), teamIdToLeave,
            (success, message) -> {
                if (success) {
                    playerProfileRepository.updateTeamIdForPlayer(user.getUid(), null, updateSuccess -> {
                        if (updateSuccess) {
                            toastMessage.postValue("Left team successfully.");
                            currentUserIdTrigger.postValue(user.getUid());
                        } else {
                            toastMessage.postValue("Left team on server, but failed to update local profile.");
                            // TODO: Handle inconsistency
                        }
                        isLoading.postValue(false);
                    });
                } else {
                    toastMessage.postValue("Failed to leave team: " + message);
                    isLoading.postValue(false);
                }
        });
    }
}
