package com.adventure.solo.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.adventure.solo.model.Clue;
import java.util.List;

@Dao
public interface ClueDao {
    @Insert
    void insert(Clue clue);

    @Insert
    void insertAll(List<Clue> clues);

    @Update
    void update(Clue clue);

    @Delete
    void delete(Clue clue);

    @Query("SELECT * FROM clues WHERE questId = :questId ORDER BY sequenceNumber")
    List<Clue> getCluesForQuest(long questId);

    @Query("SELECT * FROM clues WHERE questId = :questId AND discovered = 0 ORDER BY sequenceNumber LIMIT 1")
    Clue getNextUndiscoveredClue(long questId);

    @Query("SELECT * FROM clues WHERE questId = :questId AND discovered = 0 ORDER BY sequenceNumber")
    List<Clue> getUndiscoveredClues(long questId);

    @Query("UPDATE clues SET discovered = :discovered WHERE id = :clueId")
    void updateDiscoveredStatus(long clueId, boolean discovered);

    @Query("SELECT * FROM clues WHERE id = :clueId")
    Clue getClueById(long clueId);
} 