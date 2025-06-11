package com.adventure.solo.repository;

import com.adventure.solo.model.Quest;
import com.adventure.solo.database.QuestDao;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class QuestRepository {
    private final QuestDao questDao;

    @Inject
    public QuestRepository(QuestDao questDao) {
        this.questDao = questDao;
    }

    public long insert(Quest quest) {
        return questDao.insert(quest);
    }

    public void update(Quest quest) {
        questDao.update(quest);
    }

    public void delete(long questId) {
        questDao.delete(questId);
    }

    public Quest getQuest(long questId) {
        return questDao.getQuest(questId);
    }

    public List<Quest> getActiveQuests() {
        return questDao.getActiveQuests();
    }

    public List<Quest> getAllQuests() {
        return questDao.getAllQuests();
    }
} 