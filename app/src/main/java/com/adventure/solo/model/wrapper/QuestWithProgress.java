package com.adventure.solo.model.wrapper;
import com.adventure.solo.model.Quest;
import com.adventure.solo.model.QuestProgress;

public class QuestWithProgress {
    public Quest quest;
    public QuestProgress progress; // Can be null if no progress yet for this team

    public QuestWithProgress(Quest quest, QuestProgress progress) {
        this.quest = quest;
        this.progress = progress;
    }
}
