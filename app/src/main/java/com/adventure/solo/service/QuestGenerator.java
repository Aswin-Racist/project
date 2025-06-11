package com.adventure.solo.service;

import android.location.Location;
import com.adventure.solo.model.Quest;
import com.adventure.solo.model.Clue;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class QuestGenerator {
    private final Random random;
    private final String[] questTypes = {"EXPLORE", "RIDDLE", "CHALLENGE"};
    private final String[] clueTypes = {"LOCATION", "RIDDLE", "TASK"};
    
    private final String[] questTemplates = {
        "Discover the hidden %s",
        "The mystery of the %s",
        "Journey to the ancient %s",
        "Search for the lost %s",
        "The secret of the %s"
    };
    
    private final String[] locationNouns = {
        "temple", "garden", "statue", "fountain", "monument",
        "bridge", "tower", "plaza", "market", "park"
    };

    @Inject
    public QuestGenerator() {
        this.random = new Random();
    }

    public Quest generateQuest(Location userLocation, int difficulty) {
        String questType = questTypes[random.nextInt(questTypes.length)];
        String locationNoun = locationNouns[random.nextInt(locationNouns.length)];
        String title = String.format(
            questTemplates[random.nextInt(questTemplates.length)],
            locationNoun
        );

        // Generate a location within 1km of the user
        double radius = 0.01; // Roughly 1km
        double questLat = userLocation.getLatitude() + (random.nextDouble() * 2 - 1) * radius;
        double questLong = userLocation.getLongitude() + (random.nextDouble() * 2 - 1) * radius;

        String description = generateQuestDescription(questType, locationNoun);
        int rewardPoints = calculateRewardPoints(difficulty);

        return new Quest(title, description, questLat, questLong, difficulty, rewardPoints, questType);
    }

    public List<Clue> generateClues(Quest quest, int numberOfClues) {
        List<Clue> clues = new ArrayList<>();
        double baseLatitude = quest.getStartLatitude();
        double baseLongitude = quest.getStartLongitude();

        for (int i = 0; i < numberOfClues; i++) {
            // Generate location within 100m of the quest location
            double radius = 0.001; // Roughly 100m
            double clueLat = baseLatitude + (random.nextDouble() * 2 - 1) * radius;
            double clueLong = baseLongitude + (random.nextDouble() * 2 - 1) * radius;

            String clueType = clueTypes[random.nextInt(clueTypes.length)];
            String clueText = generateClueText(quest.getQuestType(), i + 1, numberOfClues);
            String hint = generateHint(clueType, i + 1);

            clues.add(new Clue(quest.getId(), clueText, clueLat, clueLong, i + 1, clueType, hint));
        }

        return clues;
    }

    private String generateQuestDescription(String questType, String locationNoun) {
        switch (questType) {
            case "EXPLORE":
                return "Explore the mysterious " + locationNoun + " and uncover its secrets. " +
                       "Follow the clues to complete your journey.";
            case "RIDDLE":
                return "Solve the riddles of the " + locationNoun + ". " +
                       "Each clue will lead you closer to the truth.";
            case "CHALLENGE":
                return "Face the challenges of the " + locationNoun + ". " +
                       "Test your wit and courage through various tasks.";
            default:
                return "Embark on an adventure to the " + locationNoun + ".";
        }
    }

    private String generateClueText(String questType, int currentClue, int totalClues) {
        if (currentClue == totalClues) {
            return "This is your final destination. The quest ends here.";
        }

        switch (questType) {
            case "EXPLORE":
                return "Look around carefully. Your next clue awaits at a nearby landmark.";
            case "RIDDLE":
                return generateRiddle(currentClue);
            case "CHALLENGE":
                return "Complete this challenge to reveal the next location.";
            default:
                return "Follow the markers to your next destination.";
        }
    }

    private String generateRiddle(int clueNumber) {
        String[] riddles = {
            "What walks on four legs in the morning, two legs in the afternoon, and three legs in the evening?",
            "I speak without a mouth and hear without ears. I have no body, but I come alive with wind. What am I?",
            "The more you take, the more you leave behind. What am I?",
            "What has keys, but no locks; space, but no room; and you can enter, but not go in?"
        };
        return riddles[clueNumber % riddles.length];
    }

    private String generateHint(String clueType, int clueNumber) {
        switch (clueType) {
            case "LOCATION":
                return "Look for distinctive landmarks or features in the area.";
            case "RIDDLE":
                return "Think metaphorically. The answer might not be literal.";
            case "TASK":
                return "Complete the task to unlock the next clue.";
            default:
                return "Keep exploring to find the next clue.";
        }
    }

    private int calculateRewardPoints(int difficulty) {
        return 100 * difficulty + random.nextInt(50 * difficulty);
    }
} 