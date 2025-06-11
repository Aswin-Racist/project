package com.demo.map.model;

import android.location.Location;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Player {
    private String id;
    private String name;
    private String avatarResource;
    private int totalPoints;
    private int completedMissions;
    private final List<Mission> activeMissions;
    private final List<GameReward> collectedRewards;
    private Location lastLocation;
    private long lastLocationUpdate;
    private float totalDistanceTraveled;
    private long playStartTime;
    private final Map<Achievement.Type, Achievement> achievements;
    private double latitude;
    private double longitude;
    private PlayerStats stats;
    private boolean isCurrentPlayer;
    private int score;
    private String avatarName;

    public Player(String id, String name, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.totalPoints = 0;
        this.completedMissions = 0;
        this.activeMissions = new ArrayList<>();
        this.collectedRewards = new ArrayList<>();
        this.lastLocationUpdate = 0;
        this.totalDistanceTraveled = 0;
        this.playStartTime = System.currentTimeMillis();
        this.achievements = initializeAchievements();
        this.stats = new PlayerStats();
        this.isCurrentPlayer = false;
        this.latitude = latitude;
        this.longitude = longitude;
        this.score = 0;
        this.avatarName = "default_avatar.png";
    }

    public Player(String id, String name) {
        this(id, name, 0.0, 0.0);
    }

    private Map<Achievement.Type, Achievement> initializeAchievements() {
        Map<Achievement.Type, Achievement> map = new HashMap<>();
        map.put(Achievement.Type.FIRST_REWARD, new Achievement(Achievement.Type.FIRST_REWARD, 1));
        map.put(Achievement.Type.MISSION_MASTER, new Achievement(Achievement.Type.MISSION_MASTER, 5));
        map.put(Achievement.Type.GOLD_DIGGER, new Achievement(Achievement.Type.GOLD_DIGGER, 5));
        map.put(Achievement.Type.TREASURE_KING, new Achievement(Achievement.Type.TREASURE_KING, 3));
        map.put(Achievement.Type.COIN_COLLECTOR, new Achievement(Achievement.Type.COIN_COLLECTOR, 20));
        map.put(Achievement.Type.EXPLORER, new Achievement(Achievement.Type.EXPLORER, 1000));
        map.put(Achievement.Type.SOCIAL_BUTTERFLY, new Achievement(Achievement.Type.SOCIAL_BUTTERFLY, 5));
        map.put(Achievement.Type.SPEED_RUNNER, new Achievement(Achievement.Type.SPEED_RUNNER, 1));
        map.put(Achievement.Type.TOP_SCORER, new Achievement(Achievement.Type.TOP_SCORER, 1000));
        map.put(Achievement.Type.DEDICATED_PLAYER, new Achievement(Achievement.Type.DEDICATED_PLAYER, 3600000)); // 1 hour in ms
        return map;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatarResource() {
        return avatarResource;
    }

    public void setAvatarResource(String avatarResource) {
        this.avatarResource = avatarResource;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public void addPoints(int points) {
        this.totalPoints += points;
        checkPointsAchievements();
    }

    public int getCompletedMissions() {
        return completedMissions;
    }

    public void incrementCompletedMissions() {
        this.completedMissions++;
        achievements.get(Achievement.Type.MISSION_MASTER).incrementProgress();
    }

    public List<Mission> getActiveMissions() {
        return activeMissions;
    }

    public void addMission(Mission mission) {
        activeMissions.add(mission);
    }

    public void removeMission(Mission mission) {
        activeMissions.remove(mission);
    }

    public List<GameReward> getCollectedRewards() {
        return collectedRewards;
    }

    public void addCollectedReward(GameReward reward) {
        collectedRewards.add(reward);
        addPoints(reward.getPoints());
        
        // Update achievements
        if (collectedRewards.size() == 1) {
            achievements.get(Achievement.Type.FIRST_REWARD).incrementProgress();
        }
        
        switch (reward.getType()) {
            case COIN:
                achievements.get(Achievement.Type.COIN_COLLECTOR).incrementProgress();
                break;
            case GOLD_BAR:
                achievements.get(Achievement.Type.GOLD_DIGGER).incrementProgress();
                break;
            case TREASURE_CHEST:
                achievements.get(Achievement.Type.TREASURE_KING).incrementProgress();
                break;
        }
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public void updateLocation(Location location) {
        if (lastLocation != null) {
            float distance = lastLocation.distanceTo(location);
            totalDistanceTraveled += distance;
            achievements.get(Achievement.Type.EXPLORER).updateProgress(totalDistanceTraveled);
        }
        this.lastLocation = location;
        this.lastLocationUpdate = System.currentTimeMillis();
    }

    public long getLastLocationUpdate() {
        return lastLocationUpdate;
    }

    public boolean isLocationStale() {
        return System.currentTimeMillis() - lastLocationUpdate > 60000; // 1 minute
    }

    public float getTotalDistanceTraveled() {
        return totalDistanceTraveled;
    }

    public long getPlayTime() {
        return System.currentTimeMillis() - playStartTime;
    }

    public List<Achievement> getUnlockedAchievements() {
        List<Achievement> unlocked = new ArrayList<>();
        for (Achievement achievement : achievements.values()) {
            if (achievement.isUnlocked()) {
                unlocked.add(achievement);
            }
        }
        return unlocked;
    }

    public List<Achievement> getInProgressAchievements() {
        List<Achievement> inProgress = new ArrayList<>();
        for (Achievement achievement : achievements.values()) {
            if (!achievement.isUnlocked()) {
                inProgress.add(achievement);
            }
        }
        return inProgress;
    }

    public void updateNearbyPlayersCount(int count) {
        achievements.get(Achievement.Type.SOCIAL_BUTTERFLY).updateProgress(count);
    }

    private void checkPointsAchievements() {
        achievements.get(Achievement.Type.TOP_SCORER).updateProgress(totalPoints);
    }

    public void updatePlayTime() {
        long playTime = getPlayTime();
        achievements.get(Achievement.Type.DEDICATED_PLAYER).updateProgress(playTime);
    }

    public void checkMissionCompletionTime(Mission mission) {
        long completionTime = System.currentTimeMillis() - mission.getCreationTime();
        if (completionTime <= 300000) { // 5 minutes
            achievements.get(Achievement.Type.SPEED_RUNNER).incrementProgress();
        }
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public PlayerStats getStats() {
        return stats;
    }

    public List<Achievement> getAchievements() {
        List<Achievement> achievementList = new ArrayList<>();
        achievementList.addAll(achievements.values());
        return achievementList;
    }

    public void addAchievement(Achievement achievement) {
        achievements.put(achievement.getType(), achievement);
        stats.addAchievementPoints(achievement.getType().getPoints());
    }

    public boolean isCurrentPlayer() {
        return isCurrentPlayer;
    }

    public void setCurrentPlayer(boolean currentPlayer) {
        isCurrentPlayer = currentPlayer;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getAvatarName() {
        return avatarName;
    }

    public void setAvatarName(String avatarName) {
        this.avatarName = avatarName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return id.equals(player.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
} 