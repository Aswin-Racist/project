package com.adventure.solo.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.adventure.solo.model.Quest;
import java.util.List;

import androidx.lifecycle.LiveData; // Added for consistency with prompt example
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy; // Added for consistency
import androidx.room.Query;
import androidx.room.Update;
import com.adventure.solo.model.Quest;
// import com.adventure.solo.model.QuestStatus; // Not used here based on current methods

import java.util.List;

@Dao
public interface QuestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) // Added OnConflictStrategy
    void insertQuest(Quest quest); // Kept original void insert for other uses

    @Insert(onConflict = OnConflictStrategy.REPLACE) // Added OnConflictStrategy
    long insertQuestAndGetId(Quest quest); // New method for returning ID

    @Query("SELECT * FROM quests")
    LiveData<List<Quest>> getAllQuestsLiveData(); // Kept from prompt example

    @Query("SELECT * FROM quests")
    List<Quest> getAllQuestsNonLiveData(); // Renamed from getAllQuests

    @Query("SELECT * FROM quests WHERE id = :questId")
    Quest getQuestById(long questId); // Renamed from getQuest

    @Update
    void updateQuest(Quest quest); // Renamed from update

    @Query("UPDATE quests SET completed = :completed WHERE id = :questId")
    void updateQuestCompletedStatus(long questId, boolean completed);
} 