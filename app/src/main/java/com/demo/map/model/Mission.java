package com.demo.map.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Mission {
    private final String id;
    private final String title;
    private final String clue;
    private final List<GameReward> rewards;
    private final long creationTime;
    private boolean completed;

    public Mission(String title, String clue) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.clue = clue;
        this.rewards = new ArrayList<>();
        this.creationTime = System.currentTimeMillis();
        this.completed = false;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getClue() {
        return clue;
    }

    public List<GameReward> getRewards() {
        return rewards;
    }

    public void addReward(GameReward reward) {
        rewards.add(reward);
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public int getTotalPoints() {
        return rewards.stream()
                .filter(GameReward::isCollected)
                .mapToInt(GameReward::getPoints)
                .sum();
    }
} 