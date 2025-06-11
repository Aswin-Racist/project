package com.adventure.solo.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "quests")
public class Quest {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String title;
    private String description;
    private double startLatitude;
    private double startLongitude;
    private int difficulty; // 1-5
    private boolean completed;
    private int rewardPoints;
    private String questType; // "EXPLORE", "RIDDLE", "CHALLENGE"

    public Quest(String title, String description, double startLatitude, 
                double startLongitude, int difficulty, int rewardPoints, String questType) {
        this.title = title;
        this.description = description;
        this.startLatitude = startLatitude;
        this.startLongitude = startLongitude;
        this.difficulty = difficulty;
        this.rewardPoints = rewardPoints;
        this.questType = questType;
        this.completed = false;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public double getStartLatitude() { return startLatitude; }
    public void setStartLatitude(double startLatitude) { this.startLatitude = startLatitude; }
    
    public double getStartLongitude() { return startLongitude; }
    public void setStartLongitude(double startLongitude) { this.startLongitude = startLongitude; }
    
    public int getDifficulty() { return difficulty; }
    public void setDifficulty(int difficulty) { this.difficulty = difficulty; }
    
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    
    public int getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(int rewardPoints) { this.rewardPoints = rewardPoints; }
    
    public String getQuestType() { return questType; }
    public void setQuestType(String questType) { this.questType = questType; }
} 