package com.adventure.solo.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.adventure.solo.model.ClueProgress;
import java.util.List;

@Dao
public interface ClueProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(ClueProgress clueProgress);

    // Get all clue progresses for a specific quest and team
    @Query("SELECT * FROM clue_progress WHERE questId = :questId AND teamId = :teamId")
    List<ClueProgress> getProgressForQuestByTeam(long questId, String teamId); // questId is long

    // Get a specific clue's progress for a team
    @Query("SELECT * FROM clue_progress WHERE actualClueId = :actualClueId AND teamId = :teamId")
    ClueProgress getClueProgress(long actualClueId, String teamId);

    // Update a specific clue's discovered status for a team
    @Query("UPDATE clue_progress SET discoveredByTeam = :discovered, discoveredByPlayerId = :playerId WHERE actualClueId = :actualClueId AND teamId = :teamId")
    void updateDiscovered(long actualClueId, String teamId, boolean discovered, String playerId);
}
