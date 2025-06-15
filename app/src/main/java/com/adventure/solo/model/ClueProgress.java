package com.adventure.solo.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.annotation.NonNull;
import static androidx.room.ForeignKey.CASCADE;

@Entity(tableName = "clue_progress",
        primaryKeys = {"actualClueId", "teamId"}, // Composite primary key
        indices = {
            @Index("actualClueId"),
            @Index("teamId"),
            @Index("questId") // Index on questId for faster queries of clues by quest
        }
        // Example ForeignKeys if Quest and Clue were both Room entities and linked
        // foreignKeys = {
        //     @ForeignKey(entity = Quest.class, parentColumns = "id", childColumns = "questId", onDelete = CASCADE),
        //     @ForeignKey(entity = Clue.class, parentColumns = "id", childColumns = "actualClueId", onDelete = CASCADE)
        // }
    )
public class ClueProgress {

    public long actualClueId; // Part of PK, matches Clue.id (which is long)

    @NonNull
    public String teamId; // Part of PK, from Firebase Team.teamId

    public long questId; // The quest this clue belongs to (matches Quest.id)

    public boolean discoveredByTeam;
    public String discoveredByPlayerId; // Firebase UID of the first player in the team to discover it

    // Constructor
    public ClueProgress(long actualClueId, @NonNull String teamId, long questId) {
        this.actualClueId = actualClueId;
        this.teamId = teamId;
        this.questId = questId;
        this.discoveredByTeam = false; // Default
    }

    // Getters and Setters (optional for Room with public fields)
    public long getActualClueId() {
        return actualClueId;
    }

    public void setActualClueId(long actualClueId) {
        this.actualClueId = actualClueId;
    }

    @NonNull
    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(@NonNull String teamId) {
        this.teamId = teamId;
    }

    public long getQuestId() {
        return questId;
    }

    public void setQuestId(long questId) {
        this.questId = questId;
    }

    public boolean isDiscoveredByTeam() {
        return discoveredByTeam;
    }

    public void setDiscoveredByTeam(boolean discoveredByTeam) {
        this.discoveredByTeam = discoveredByTeam;
    }

    public String getDiscoveredByPlayerId() {
        return discoveredByPlayerId;
    }

    public void setDiscoveredByPlayerId(String discoveredByPlayerId) {
        this.discoveredByPlayerId = discoveredByPlayerId;
    }
}
