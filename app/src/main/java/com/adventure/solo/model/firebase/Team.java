package com.adventure.solo.model.firebase;

import java.util.List;
import java.util.ArrayList;

// POJO for Firebase (Realtime Database or Firestore)
public class Team {
    public String teamId;
    public String teamName;
    public String teamLeaderPlayerId; // Firebase UID of the leader
    public List<String> memberPlayerIds; // List of Firebase UIDs

    // Default constructor required for calls to DataSnapshot.getValue(Team.class)
    public Team() {
        this.memberPlayerIds = new ArrayList<>();
    }

    public Team(String teamId, String teamName, String teamLeaderPlayerId) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.teamLeaderPlayerId = teamLeaderPlayerId;
        this.memberPlayerIds = new ArrayList<>();
        if (teamLeaderPlayerId != null && !teamLeaderPlayerId.isEmpty()) {
            this.memberPlayerIds.add(teamLeaderPlayerId); // Leader is initially a member
        }
    }
    // Getters and setters can be added if needed, or public fields used for Firebase
    // For Firebase, public fields are often sufficient and simpler.

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getTeamLeaderPlayerId() {
        return teamLeaderPlayerId;
    }

    public void setTeamLeaderPlayerId(String teamLeaderPlayerId) {
        this.teamLeaderPlayerId = teamLeaderPlayerId;
    }

    public List<String> getMemberPlayerIds() {
        return memberPlayerIds;
    }

    public void setMemberPlayerIds(List<String> memberPlayerIds) {
        this.memberPlayerIds = memberPlayerIds;
    }

    public void addMember(String playerId) {
        if (this.memberPlayerIds == null) {
            this.memberPlayerIds = new ArrayList<>();
        }
        if (playerId != null && !playerId.isEmpty() && !this.memberPlayerIds.contains(playerId)) {
            this.memberPlayerIds.add(playerId);
        }
    }

    public void removeMember(String playerId) {
        if (this.memberPlayerIds != null && playerId != null) {
            this.memberPlayerIds.remove(playerId);
        }
    }
}
