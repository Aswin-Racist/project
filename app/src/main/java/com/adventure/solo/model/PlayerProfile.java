package com.adventure.solo.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "player_profiles")
public class PlayerProfile {
    @PrimaryKey
    @NonNull
    public String firebaseUid; // Matches Firebase User UID

    public String username;
    public String teamId; // Can be null if not in a team

    public int individualXP;
    public int stamina;
    public int coins;

    // Constructor for Room and general use
    public PlayerProfile(@NonNull String firebaseUid, String username) {
        this.firebaseUid = firebaseUid;
        this.username = username;
        this.stamina = 100; // Default stamina
        this.coins = 0;     // Default coins
        this.individualXP = 0; // Default XP
        // teamId is null by default
    }

    // Getters and Setters (optional for Room if fields are public, but good practice)
    @NonNull
    public String getFirebaseUid() {
        return firebaseUid;
    }

    public void setFirebaseUid(@NonNull String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public int getIndividualXP() {
        return individualXP;
    }

    public void setIndividualXP(int individualXP) {
        this.individualXP = individualXP;
    }

    public int getStamina() {
        return stamina;
    }

    public void setStamina(int stamina) {
        this.stamina = stamina;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }
}
