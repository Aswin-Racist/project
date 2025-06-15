package com.adventure.solo.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.annotation.NonNull; // For NonNull annotation
import static androidx.room.ForeignKey.CASCADE; // If using FKs to Room entities

// Composite primary key defined in @Entity
@Entity(tableName = "quest_progress",
        primaryKeys = {"questId", "teamId"}, // teamId from Firebase, questId from local Quest
        indices = {@Index("questId"), @Index("teamId")}
        // Example ForeignKey if Quest table was local and had a matching questId
        // foreignKeys = @ForeignKey(entity = Quest.class,
        //                            parentColumns = "id", // Assuming Quest has 'id' as PK
        //                            childColumns = "questId",
        //                            onDelete = CASCADE) // Define action on Quest deletion
       )
public class QuestProgress {

    public long questId; // Matches Quest.id which is long

    @NonNull
    public String teamId; // From Firebase Team.teamId

    @NonNull // QuestStatus itself should not be null
    public QuestStatus status;

    // Player who completed the last step or the quest for the team (optional Firebase UID)
    public String lastCompletedByPlayerId;

    public QuestProgress(long questId, @NonNull String teamId) {
        this.questId = questId;
        this.teamId = teamId;
        this.status = QuestStatus.NOT_STARTED; // Default status
    }

    // Getters and Setters (optional for Room with public fields, but good practice)
    public long getQuestId() {
        return questId;
    }

    public void setQuestId(long questId) {
        this.questId = questId;
    }

    @NonNull
    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(@NonNull String teamId) {
        this.teamId = teamId;
    }

    @NonNull
    public QuestStatus getStatus() {
        return status;
    }

    public void setStatus(@NonNull QuestStatus status) {
        this.status = status;
    }

    public String getLastCompletedByPlayerId() {
        return lastCompletedByPlayerId;
    }

    public void setLastCompletedByPlayerId(String lastCompletedByPlayerId) {
        this.lastCompletedByPlayerId = lastCompletedByPlayerId;
    }
}
