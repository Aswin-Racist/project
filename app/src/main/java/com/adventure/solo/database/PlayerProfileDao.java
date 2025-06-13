package com.adventure.solo.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.adventure.solo.model.PlayerProfile;
import java.util.List;

@Dao
public interface PlayerProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(PlayerProfile playerProfile);

    @Query("SELECT * FROM player_profiles WHERE firebaseUid = :uid")
    PlayerProfile getByFirebaseUid(String uid);

    @Query("SELECT * FROM player_profiles WHERE teamId = :teamId")
    List<PlayerProfile> getPlayersByTeamId(String teamId);

    @Update
    void update(PlayerProfile playerProfile);
}
