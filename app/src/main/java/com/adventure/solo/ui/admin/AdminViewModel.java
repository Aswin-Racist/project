package com.adventure.solo.ui.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
// import androidx.lifecycle.Transformations; // Not used in this version

import com.adventure.solo.model.PlayerProfile;
import com.adventure.solo.model.firebase.Team;
import com.adventure.solo.repository.PlayerProfileRepository;
import com.adventure.solo.repository.TeamRepository;
// import com.google.firebase.auth.FirebaseAuth; // Not directly used in VM for admin check logic

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import android.util.Log;

@HiltViewModel
public class AdminViewModel extends ViewModel {
    private static final String TAG = "AdminViewModel";

    private final TeamRepository teamRepository;
    private final PlayerProfileRepository playerProfileRepository;

    private final MutableLiveData<List<Team>> _allTeams = new MutableLiveData<>();
    public LiveData<List<Team>> allTeams = _allTeams;

    // Map: TeamID -> List of PlayerProfile objects for members
    private final MutableLiveData<Map<String, List<PlayerProfile>>> _teamMembersMap = new MutableLiveData<>(new HashMap<>());
    public LiveData<Map<String, List<PlayerProfile>>> teamMembersMap = _teamMembersMap;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public LiveData<Boolean> isLoading = _isLoading;

    private final MutableLiveData<String> _toastMessage = new MutableLiveData<>();
    public LiveData<String> toastMessage = _toastMessage;
    public void clearToastMessage() { _toastMessage.setValue(null); }


    @Inject
    public AdminViewModel(TeamRepository teamRepository, PlayerProfileRepository playerProfileRepository) {
        this.teamRepository = teamRepository;
        this.playerProfileRepository = playerProfileRepository;
        fetchAllTeamsAndMembers();
    }

    public void fetchAllTeamsAndMembers() {
        _isLoading.setValue(true);
        teamRepository.getAllTeams(teamsResult -> { // Using TeamRepository.AllTeamsCallback
            if (teamsResult != null) {
                _allTeams.postValue(teamsResult); // Post value as this might be from a background thread in repo
                fetchMembersForTeams(teamsResult);
            } else {
                _allTeams.postValue(new ArrayList<>());
                _teamMembersMap.postValue(new HashMap<>()); // Clear members map too
                _isLoading.postValue(false);
                _toastMessage.postValue("Failed to fetch teams.");
            }
        });
    }

    private void fetchMembersForTeams(List<Team> teams) {
        Map<String, List<PlayerProfile>> newTeamMembersMap = new HashMap<>();
        if (teams == null || teams.isEmpty()) {
             _teamMembersMap.postValue(newTeamMembersMap); // Post empty map
             _isLoading.postValue(false); // All loading done (no teams or members to fetch)
             return;
        }

        final int[] teamsProcessedCounter = {0}; // Effectively final array for use in lambda

        for (Team team : teams) {
            if (team.memberPlayerIds != null && !team.memberPlayerIds.isEmpty()) {
                // This requires a method in PlayerProfileRepository to fetch multiple profiles by UIDs
                // Or, if PlayerProfile has teamId, getPlayersByTeamId might work if it's up-to-date
                // For now, using getPlayersByTeamId as per prompt.
                playerProfileRepository.getPlayersByTeamId(team.teamId, profiles -> {
                    newTeamMembersMap.put(team.teamId, profiles != null ? profiles : new ArrayList<>());
                    teamsProcessedCounter[0]++;
                    if (teamsProcessedCounter[0] == teams.size()) {
                        _teamMembersMap.postValue(newTeamMembersMap);
                        _isLoading.postValue(false); // All loading finished
                    }
                });
            } else {
                newTeamMembersMap.put(team.teamId, new ArrayList<>()); // No members
                teamsProcessedCounter[0]++;
                if (teamsProcessedCounter[0] == teams.size()) {
                    _teamMembersMap.postValue(newTeamMembersMap);
                    _isLoading.postValue(false); // All loading finished
                }
            }
        }
    }


    public void assignPlayerToTeam(String playerUid, String targetTeamId, String playerUsername) {
        if (playerUid == null || playerUid.trim().isEmpty() || targetTeamId == null || targetTeamId.trim().isEmpty()) {
            _toastMessage.setValue("Player UID and Team ID cannot be empty.");
            return;
        }
        _isLoading.setValue(true);
        // Use a default username if null, or fetch from PlayerProfile if necessary (more complex)
        String usernameForTeam = (playerUsername != null && !playerUsername.isEmpty()) ? playerUsername : "New Member";

        teamRepository.addPlayerToTeam(playerUid, usernameForTeam, targetTeamId, (success, message) -> {
            if (success) {
                playerProfileRepository.updateTeamIdForPlayer(playerUid, targetTeamId, profileUpdateSuccess -> {
                    if (profileUpdateSuccess) {
                        _toastMessage.postValue("Player assigned successfully.");
                        fetchAllTeamsAndMembers(); // Refresh data
                    } else {
                        _toastMessage.postValue("Player added to Firebase team, but local profile update failed for UID: " + playerUid);
                        // TODO: Consider rollback logic for Firebase if critical
                    }
                    _isLoading.postValue(false);
                });
            } else {
                _toastMessage.postValue("Failed to assign player: " + message);
                _isLoading.postValue(false);
            }
        });
    }

    public void disbandTeam(String teamId) {
        if (teamId == null || teamId.trim().isEmpty()) {
            _toastMessage.setValue("Team ID cannot be empty.");
            return;
        }
        _isLoading.setValue(true);

        teamRepository.getTeam(teamId, team -> { // Fetch team details first
            if (team != null && team.memberPlayerIds != null) {
                final int[] membersToUpdate = {team.memberPlayerIds.size()};
                if (membersToUpdate[0] == 0) { // No members, just delete team
                    deleteFirebaseTeamAndRefresh(teamId);
                    return;
                }
                for (String memberUid : team.memberPlayerIds) {
                    playerProfileRepository.updateTeamIdForPlayer(memberUid, null, updateSuccess -> {
                        if (!updateSuccess) {
                            Log.e(TAG, "Failed to clear teamId for member: " + memberUid + " from team " + teamId);
                            // Continue trying to update other members
                        }
                        membersToUpdate[0]--;
                        if (membersToUpdate[0] == 0) { // All members processed
                            deleteFirebaseTeamAndRefresh(teamId);
                        }
                    });
                }
            } else if (team != null) { // Team exists but has no members list (or it's null)
                 deleteFirebaseTeamAndRefresh(teamId); // Just delete the team
            }
            else { // Team not found in Firebase
                _toastMessage.postValue("Team not found in Firebase, cannot disband.");
                _isLoading.postValue(false);
            }
        });
    }

    private void deleteFirebaseTeamAndRefresh(String teamId) {
        teamRepository.deleteTeam(teamId, (success, message) -> {
            if (success) {
                _toastMessage.postValue("Team disbanded successfully.");
                fetchAllTeamsAndMembers(); // Refresh list
            } else {
                _toastMessage.postValue("Failed to disband team from Firebase: " + message);
            }
            _isLoading.postValue(false);
        });
    }
}
