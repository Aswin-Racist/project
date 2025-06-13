package com.adventure.solo.repository;

import com.adventure.solo.model.firebase.Team;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;
// import java.util.UUID; // Not needed if Firebase push keys are used for teamId

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import android.util.Log;

@Singleton
public class TeamRepository {
    private static final String TAG = "TeamRepository";
    private final DatabaseReference teamsRef;

    // Callback for create, join, leave operations
    public interface TeamOperationCallback { void onComplete(boolean success, String messageOrId); }
    // Callback for fetching a single team
    public interface TeamDataCallback { void onComplete(Team team); }
    // Callback for checking if a team name exists
    public interface TeamNameExistsCallback { void onComplete(boolean exists, String message); }
    // Callback for fetching all teams
    public interface AllTeamsCallback { void onComplete(List<Team> teams); }


    @Inject
    public TeamRepository(FirebaseDatabase firebaseDatabase) {
        this.teamsRef = firebaseDatabase.getReference("teams");
    }

    public void createTeam(String teamName, String leaderPlayerId, String leaderUsername, TeamOperationCallback callback) {
        // Check if team name already exists to prevent duplicates
        teamsRef.orderByChild("teamName").equalTo(teamName).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Team name already taken
                    callback.onComplete(false, "Team name '" + teamName + "' is already taken.");
                } else {
                    // Team name is available, proceed to create the team
                    String teamId = teamsRef.push().getKey();
                    if (teamId == null) {
                        callback.onComplete(false, "Failed to generate unique team ID.");
                        return;
                    }
                    Team newTeam = new Team(teamId, teamName, leaderPlayerId);
                    // If storing display names in a map within Team object (e.g., memberDisplayNames)
                    // if (leaderUsername != null) newTeam.getMemberDisplayNames().put(leaderPlayerId, leaderUsername);

                    teamsRef.child(teamId).setValue(newTeam)
                        .addOnSuccessListener(aVoid -> callback.onComplete(true, teamId))
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to create team: " + e.getMessage());
                            callback.onComplete(false, e.getMessage());
                        });
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Database error while checking team name: " + databaseError.getMessage());
                callback.onComplete(false, databaseError.getMessage());
            }
        });
    }

    public void getTeam(String teamId, TeamDataCallback callback) {
        if (teamId == null || teamId.isEmpty()) {
            Log.e(TAG, "getTeam called with null or empty teamId");
            callback.onComplete(null); // Or an error state
            return;
        }
        teamsRef.child(teamId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                callback.onComplete(snapshot.exists() ? snapshot.getValue(Team.class) : null);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to get team " + teamId + ": " + error.getMessage());
                callback.onComplete(null);
            }
        });
    }

    public void addPlayerToTeam(String playerId, String playerUsername, String teamId, TeamOperationCallback callback) {
        if (teamId == null || teamId.isEmpty() || playerId == null || playerId.isEmpty()) {
            callback.onComplete(false, "Team ID or Player ID cannot be null or empty.");
            return;
        }
        DatabaseReference teamNodeRef = teamsRef.child(teamId);
        teamNodeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    callback.onComplete(false, "Team not found.");
                    return;
                }
                Team team = snapshot.getValue(Team.class);
                if (team != null) {
                    if (team.memberPlayerIds == null) team.memberPlayerIds = new ArrayList<>();
                    if (!team.memberPlayerIds.contains(playerId)) {
                        team.memberPlayerIds.add(playerId);
                        // If storing display names:
                        // if (team.getMemberDisplayNames() == null) team.setMemberDisplayNames(new HashMap<>());
                        // if (playerUsername != null) team.getMemberDisplayNames().put(playerId, playerUsername);

                        teamNodeRef.setValue(team) // Update the whole team object
                            .addOnSuccessListener(aVoid -> callback.onComplete(true, "Player added successfully."))
                            .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
                    } else {
                        callback.onComplete(false, "Player already in this team.");
                    }
                } else {
                    callback.onComplete(false, "Could not deserialize team data.");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to add player to team " + teamId + ": " + error.getMessage());
                callback.onComplete(false, error.getMessage());
            }
        });
    }

    public void removePlayerFromTeam(String playerId, String teamId, TeamOperationCallback callback) {
         if (teamId == null || teamId.isEmpty() || playerId == null || playerId.isEmpty()) {
            callback.onComplete(false, "Team ID or Player ID cannot be null or empty.");
            return;
        }
        DatabaseReference teamNodeRef = teamsRef.child(teamId);
        teamNodeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    callback.onComplete(false, "Team not found.");
                    return;
                }
                Team team = snapshot.getValue(Team.class);
                if (team != null && team.memberPlayerIds != null) {
                    if (playerId.equals(team.teamLeaderPlayerId) && team.memberPlayerIds.size() > 1) {
                        // More complex logic: If leader leaves and others remain, must assign new leader or disband.
                        // For now, prevent leader from leaving if others are present via this simple method.
                        callback.onComplete(false, "Team leader cannot leave directly if other members exist. Promote new leader or disband.");
                        return;
                    }

                    boolean removed = team.memberPlayerIds.remove(playerId);
                    // if (team.getMemberDisplayNames() != null) team.getMemberDisplayNames().remove(playerId);

                    if (removed) {
                        if (team.memberPlayerIds.isEmpty()) {
                            // Last member (must be leader) leaving, so delete the team.
                            teamNodeRef.removeValue()
                                .addOnSuccessListener(aVoid -> callback.onComplete(true, "Player removed and team disbanded."))
                                .addOnFailureListener(e -> callback.onComplete(false, "Player removed, but failed to disband empty team: "+e.getMessage()));
                        } else {
                            teamNodeRef.setValue(team) // Update the whole team object
                                .addOnSuccessListener(aVoid -> callback.onComplete(true, "Player removed from team."))
                                .addOnFailureListener(e -> callback.onComplete(false, e.getMessage()));
                        }
                    } else {
                        callback.onComplete(false, "Player not found in team roster.");
                    }
                } else {
                     callback.onComplete(false, "Could not read team data or member list is null.");
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                 Log.e(TAG, "Failed to remove player from team " + teamId + ": " + error.getMessage());
                 callback.onComplete(false, error.getMessage());
            }
        });
    }

    public void getAllTeams(AllTeamsCallback callback) {
       teamsRef.addListenerForSingleValueEvent(new ValueEventListener() {
           @Override
           public void onDataChange(@NonNull DataSnapshot snapshot) {
               List<Team> teamsList = new ArrayList<>();
               for (DataSnapshot teamSnapshot : snapshot.getChildren()) {
                   Team team = teamSnapshot.getValue(Team.class);
                   if (team != null) {
                       teamsList.add(team);
                   }
               }
               callback.onComplete(teamsList);
           }
           @Override
           public void onCancelled(@NonNull DatabaseError error) {
               Log.e(TAG, "Failed to get all teams: " + error.getMessage());
               callback.onComplete(new ArrayList<>()); // Return empty list on error
           }
       });
    }

    public void deleteTeam(String teamId, TeamOperationCallback callback) {
        if (teamId == null || teamId.isEmpty()) {
            callback.onComplete(false, "Team ID cannot be null or empty for deletion.");
            return;
        }
       teamsRef.child(teamId).removeValue()
           .addOnSuccessListener(aVoid -> callback.onComplete(true, "Team deleted successfully."))
           .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to delete team " + teamId + ": " + e.getMessage());
                callback.onComplete(false, e.getMessage());
           });
    }
}
