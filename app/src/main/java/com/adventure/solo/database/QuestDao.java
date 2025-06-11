package com.adventure.solo.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.adventure.solo.model.Quest;
import java.util.List;

@Dao
public interface QuestDao {
    @Insert
    long insert(Quest quest);

    @Update
    void update(Quest quest);

    @Query("DELETE FROM quests WHERE id = :questId")
    void delete(long questId);

    @Query("SELECT * FROM quests WHERE id = :questId")
    Quest getQuest(long questId);

    @Query("SELECT * FROM quests WHERE completed = 0")
    List<Quest> getActiveQuests();

    @Query("SELECT * FROM quests ORDER BY id DESC")
    List<Quest> getAllQuests();

    @Query("UPDATE quests SET completed = :completed WHERE id = :questId")
    void updateQuestCompletedStatus(long questId, boolean completed);
} 