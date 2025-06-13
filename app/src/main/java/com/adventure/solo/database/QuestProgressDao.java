package com.adventure.solo.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.adventure.solo.model.QuestProgress;
import com.adventure.solo.model.QuestStatus;
import java.util.List;

@Dao
public interface QuestProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(QuestProgress questProgress);

    @Query("SELECT * FROM quest_progress WHERE teamId = :teamId")
    List<QuestProgress> getProgressForTeam(String teamId);

    @Query("SELECT * FROM quest_progress WHERE questId = :questId AND teamId = :teamId")
    QuestProgress getQuestProgress(long questId, String teamId); // questId is long

    @Query("UPDATE quest_progress SET status = :status, lastCompletedByPlayerId = :playerId WHERE questId = :questId AND teamId = :teamId")
    void updateStatus(long questId, String teamId, QuestStatus status, String playerId); // questId is long
}
