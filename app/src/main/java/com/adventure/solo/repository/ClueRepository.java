package com.adventure.solo.repository;

import com.adventure.solo.model.Clue;
import com.adventure.solo.database.ClueDao;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ClueRepository {
    private final ClueDao clueDao;

    @Inject
    public ClueRepository(ClueDao clueDao) {
        this.clueDao = clueDao;
    }

    public void insert(Clue clue) {
        clueDao.insert(clue);
    }

    public void insertAll(List<Clue> clues) {
        clueDao.insertAll(clues);
    }

    public void update(Clue clue) {
        clueDao.update(clue);
    }

    public void delete(Clue clue) {
        clueDao.delete(clue);
    }

    public List<Clue> getCluesForQuest(long questId) {
        return clueDao.getCluesForQuest(questId);
    }

    public Clue getNextUndiscoveredClue(long questId) {
        return clueDao.getNextUndiscoveredClue(questId);
    }

    public List<Clue> getUndiscoveredClues(long questId) {
        return clueDao.getUndiscoveredClues(questId);
    }

    public void updateDiscoveredStatus(long clueId, boolean discovered) {
        clueDao.updateDiscoveredStatus(clueId, discovered);
    }

    public Clue getClueById(long clueId) {
        return clueDao.getClueById(clueId);
    }
} 