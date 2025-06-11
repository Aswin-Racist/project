package com.demo.map.model;

public class GameReward {
    public enum RewardType {
        COIN(10),
        GOLD_BAR(50),
        TREASURE_CHEST(100);

        private final int points;

        RewardType(int points) {
            this.points = points;
        }

        public int getPoints() {
            return points;
        }
    }

    private final RewardType type;
    private final double latitude;
    private final double longitude;
    private boolean collected;

    public GameReward(RewardType type, double latitude, double longitude) {
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.collected = false;
    }

    public RewardType getType() {
        return type;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public boolean isCollected() {
        return collected;
    }

    public void setCollected(boolean collected) {
        this.collected = collected;
    }

    public int getPoints() {
        return type.getPoints();
    }
} 