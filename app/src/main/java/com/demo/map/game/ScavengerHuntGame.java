package com.demo.map.game;

import android.location.Location;
import com.demo.map.model.GameReward;
import com.demo.map.model.Mission;
import com.demo.map.model.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ScavengerHuntGame {
    private static final int MAX_ACTIVE_MISSIONS = 10;
    private static final int REWARD_RADIUS = 50; // meters
    private static final String[] MISSION_TITLES = {
        "Hidden Treasures",
        "Golden Path",
        "Mystery Cache",
        "Ancient Artifacts",
        "Lost Relics"
    };
    
    private static final String[] CLUE_TEMPLATES = {
        "Look for treasures near %s",
        "Adventure awaits around %s",
        "Explore the area near %s",
        "Secrets hide close to %s",
        "Valuable rewards scattered around %s"
    };

    private final Random random;
    private final List<Player> players;
    private final List<Mission> activeMissions;
    private Player currentPlayer;

    public ScavengerHuntGame() {
        this.random = new Random();
        this.players = new ArrayList<>();
        this.activeMissions = new ArrayList<>();
        this.currentPlayer = new Player("player1", "Player 1");
        this.players.add(currentPlayer);
    }

    public Mission generateMission(Location playerLocation) {
        if (activeMissions.size() >= MAX_ACTIVE_MISSIONS) {
            return null;
        }

        String title = MISSION_TITLES[random.nextInt(MISSION_TITLES.length)];
        String locationName = getLocationName(playerLocation);
        String clue = String.format(
            CLUE_TEMPLATES[random.nextInt(CLUE_TEMPLATES.length)],
            locationName
        );

        Mission mission = new Mission(title, clue);
        
        // Generate 3-5 rewards for the mission
        int rewardCount = random.nextInt(3) + 3;
        for (int i = 0; i < rewardCount; i++) {
            Location rewardLocation = generateNearbyLocation(playerLocation);
            GameReward.RewardType type = getRandomRewardType();
            mission.addReward(new GameReward(type, rewardLocation.getLatitude(), rewardLocation.getLongitude()));
        }

        activeMissions.add(mission);
        return mission;
    }

    private GameReward.RewardType getRandomRewardType() {
        double chance = random.nextDouble();
        if (chance < 0.6) { // 60% chance for coins
            return GameReward.RewardType.COIN;
        } else if (chance < 0.9) { // 30% chance for gold bars
            return GameReward.RewardType.GOLD_BAR;
        } else { // 10% chance for treasure chests
            return GameReward.RewardType.TREASURE_CHEST;
        }
    }

    private Location generateNearbyLocation(Location center) {
        double radius = REWARD_RADIUS;
        double x0 = center.getLatitude();
        double y0 = center.getLongitude();

        // Convert radius from meters to degrees
        double radiusInDegrees = radius / 111000f;

        double u = random.nextDouble();
        double v = random.nextDouble();
        double w = radiusInDegrees * Math.sqrt(u);
        double t = 2 * Math.PI * v;
        double x = w * Math.cos(t);
        double y = w * Math.sin(t);

        Location location = new Location("");
        location.setLatitude(x0 + x);
        location.setLongitude(y0 + y);
        return location;
    }

    private String getLocationName(Location location) {
        // TODO: Implement reverse geocoding to get actual location name
        return String.format("%.6f, %.6f", location.getLatitude(), location.getLongitude());
    }

    public List<GameReward> getNearbyRewards(Location playerLocation, double maxDistance) {
        List<GameReward> nearbyRewards = new ArrayList<>();
        for (Mission mission : activeMissions) {
            for (GameReward reward : mission.getRewards()) {
                if (reward.isCollected()) {
                    continue;
                }

                Location rewardLocation = new Location("");
                rewardLocation.setLatitude(reward.getLatitude());
                rewardLocation.setLongitude(reward.getLongitude());

                float distance = playerLocation.distanceTo(rewardLocation);
                if (distance <= maxDistance) {
                    nearbyRewards.add(reward);
                }
            }
        }
        return nearbyRewards;
    }

    public void collectReward(Player player, GameReward reward) {
        if (!reward.isCollected()) {
            reward.setCollected(true);
            player.addCollectedReward(reward);
            
            // Check if mission is completed
            for (Mission mission : player.getActiveMissions()) {
                if (mission.getRewards().stream().allMatch(GameReward::isCollected)) {
                    completeMission(player, mission);
                }
            }
        }
    }

    private void completeMission(Player player, Mission mission) {
        mission.setCompleted(true);
        player.incrementCompletedMissions();
        player.removeMission(mission);
        activeMissions.remove(mission);
    }

    public List<Player> getLeaderboard() {
        List<Player> leaderboard = new ArrayList<>(players);
        Collections.sort(leaderboard, (p1, p2) -> Integer.compare(p2.getTotalPoints(), p1.getTotalPoints()));
        return leaderboard;
    }

    public void addPlayer(Player player) {
        players.add(player);
    }

    public void removePlayer(Player player) {
        players.remove(player);
    }

    public List<Mission> getActiveMissions() {
        return Collections.unmodifiableList(activeMissions);
    }

    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void updatePlayerLocation(Player player, double latitude, double longitude) {
        player.setLatitude(latitude);
        player.setLongitude(longitude);
    }
} 