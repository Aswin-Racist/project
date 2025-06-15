package com.adventure.solo.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.adventure.solo.model.Clue;
import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy; // Added
import androidx.room.Query;
import com.adventure.solo.model.Clue;
import java.util.List;

@Dao
public interface ClueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) // Added
    void insertClue(Clue clue); // Renamed

    @Insert(onConflict = OnConflictStrategy.REPLACE) // Added
    void insertAllClues(List<Clue> clues); // Renamed

    // @Update - Update method was removed in prompt, assuming it's not needed or handled by insert with REPLACE
    // void update(Clue clue);

    @Delete
    void delete(Clue clue);

    @Query("SELECT * FROM clues WHERE questId = :questId ORDER BY sequenceNumber ASC") // Added ASC
    List<Clue> getCluesByQuestIdNonLiveData(long questId); // Renamed

    @Query("SELECT * FROM clues WHERE questId = :questId AND discovered = 0 ORDER BY sequenceNumber ASC LIMIT 1") // Added ASC
    Clue getNextUndiscoveredClue(long questId);

    @Query("SELECT * FROM clues WHERE questId = :questId AND discovered = 0 ORDER BY sequenceNumber")
    List<Clue> getUndiscoveredClues(long questId);

    @Query("UPDATE clues SET discovered = :discovered WHERE id = :clueId")
    void updateDiscoveredStatus(long clueId, boolean discovered);

    @Query("SELECT * FROM clues WHERE id = :clueId")
    Clue getClueById(long clueId);
} 