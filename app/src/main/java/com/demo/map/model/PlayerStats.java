package com.demo.map.model;

import java.io.Serializable;

public class PlayerStats implements Serializable {
    private long totalDistance; // in meters
    private long playTime; // in seconds
    private int missionsCompleted;
    private int treasuresFound;
    private int achievementPoints;
    private int rank;

    public PlayerStats() {
        totalDistance = 0;
        playTime = 0;
        missionsCompleted = 0;
        treasuresFound = 0;
        achievementPoints = 0;
        rank = 0;
    }

    public void addDistance(float distance) {
        totalDistance += distance;
    }

    public void addPlayTime(long seconds) {
        playTime += seconds;
    }

    public void incrementMissionsCompleted() {
        missionsCompleted++;
    }

    public void incrementTreasuresFound() {
        treasuresFound++;
    }

    public void addAchievementPoints(int points) {
        achievementPoints += points;
    }

    public long getTotalDistance() {
        return totalDistance;
    }

    public long getPlayTime() {
        return playTime;
    }

    public int getMissionsCompleted() {
        return missionsCompleted;
    }

    public int getTreasuresFound() {
        return treasuresFound;
    }

    public int getAchievementPoints() {
        return achievementPoints;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getFormattedDistance() {
        if (totalDistance < 1000) {
            return totalDistance + "m";
        } else {
            float km = totalDistance / 1000f;
            return String.format("%.1fkm", km);
        }
    }

    public String getFormattedPlayTime() {
        long hours = playTime / 3600;
        long minutes = (playTime % 3600) / 60;
        
        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
} 