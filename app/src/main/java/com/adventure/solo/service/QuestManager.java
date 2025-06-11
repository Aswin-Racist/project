package com.adventure.solo.service;

import android.location.Location;
import com.adventure.solo.model.Quest;
import com.adventure.solo.model.Clue;
import com.adventure.solo.repository.QuestRepository;
import com.adventure.solo.repository.ClueRepository;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class QuestManager {
    private final QuestGenerator questGenerator;
    private final QuestRepository questRepository;
    private final ClueRepository clueRepository;
    private static final float PROXIMITY_RADIUS = 50; // meters
    private static final int DEFAULT_CLUES_PER_QUEST = 5;

    @Inject
    public QuestManager(QuestGenerator questGenerator,
                       QuestRepository questRepository,
                       ClueRepository clueRepository) {
        this.questGenerator = questGenerator;
        this.questRepository = questRepository;
        this.clueRepository = clueRepository;
    }

    public Quest generateNewQuest(Location userLocation, int difficulty) {
        Quest quest = questGenerator.generateQuest(userLocation, difficulty);
        long questId = questRepository.insert(quest);
        quest.setId(questId);

        List<Clue> clues = questGenerator.generateClues(quest, DEFAULT_CLUES_PER_QUEST);
        clueRepository.insertAll(clues);

        return quest;
    }

    public List<Quest> getActiveQuests() {
        return questRepository.getActiveQuests();
    }

    public List<Clue> getQuestClues(long questId) {
        return clueRepository.getCluesForQuest(questId);
    }

    public Clue getNextClue(long questId) {
        return clueRepository.getNextUndiscoveredClue(questId);
    }

    public boolean checkProximityToClue(Location userLocation, Clue clue) {
        float[] results = new float[1];
        Location.distanceBetween(
            userLocation.getLatitude(), userLocation.getLongitude(),
            clue.getTargetLatitude(), clue.getTargetLongitude(),
            results
        );
        return results[0] <= PROXIMITY_RADIUS;
    }

    public void markClueAsDiscovered(Clue clue) {
        clue.setDiscovered(true);
        clueRepository.update(clue);

        // Check if all clues are discovered
        List<Clue> undiscoveredClues = clueRepository.getUndiscoveredClues(clue.getQuestId());
        if (undiscoveredClues.isEmpty()) {
            Quest quest = questRepository.getQuest(clue.getQuestId());
            quest.setCompleted(true);
            questRepository.update(quest);
        }
    }

    public boolean isQuestComplete(long questId) {
        Quest quest = questRepository.getQuest(questId);
        return quest != null && quest.isCompleted();
    }

    public void abandonQuest(long questId) {
        questRepository.delete(questId);
        // Clues will be automatically deleted due to CASCADE delete in Room
    }
} 