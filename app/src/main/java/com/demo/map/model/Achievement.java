package com.demo.map.model;

public class Achievement {
    public enum Type {
        FIRST_REWARD("Treasure Hunter", "Collect your first reward", 50),
        MISSION_MASTER("Mission Master", "Complete 5 missions", 100),
        GOLD_DIGGER("Gold Digger", "Collect 5 gold bars", 150),
        TREASURE_KING("Treasure King", "Collect 3 treasure chests", 200),
        COIN_COLLECTOR("Coin Collector", "Collect 20 coins", 150),
        EXPLORER("Explorer", "Travel 1000 meters while playing", 100),
        SOCIAL_BUTTERFLY("Social Butterfly", "Play with 5 other players nearby", 100),
        SPEED_RUNNER("Speed Runner", "Complete a mission in under 5 minutes", 150),
        TOP_SCORER("Top Scorer", "Reach 1000 points", 200),
        DEDICATED_PLAYER("Dedicated Player", "Play for 1 hour total", 100);

        private final String title;
        private final String description;
        private final int points;

        Type(String title, String description, int points) {
            this.title = title;
            this.description = description;
            this.points = points;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public int getPoints() {
            return points;
        }
    }

    private final Type type;
    private boolean unlocked;
    private long unlockedTime;
    private float progress;
    private final float targetProgress;

    public Achievement(Type type, float targetProgress) {
        this.type = type;
        this.unlocked = false;
        this.progress = 0;
        this.targetProgress = targetProgress;
    }

    public Type getType() {
        return type;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public long getUnlockedTime() {
        return unlockedTime;
    }

    public float getProgress() {
        return progress;
    }

    public float getTargetProgress() {
        return targetProgress;
    }

    public float getProgressPercentage() {
        return (progress / targetProgress) * 100;
    }

    public void updateProgress(float newProgress) {
        if (!unlocked) {
            this.progress = Math.min(newProgress, targetProgress);
            if (this.progress >= targetProgress) {
                unlock();
            }
        }
    }

    public void incrementProgress() {
        updateProgress(progress + 1);
    }

    private void unlock() {
        if (!unlocked) {
            unlocked = true;
            unlockedTime = System.currentTimeMillis();
        }
    }
} 