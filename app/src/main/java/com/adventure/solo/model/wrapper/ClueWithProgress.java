package com.adventure.solo.model.wrapper;
import com.adventure.solo.model.Clue;
import com.adventure.solo.model.ClueProgress;

public class ClueWithProgress {
    public Clue clue;
    public ClueProgress progress; // Can be null if no progress yet for this team on this clue

    public ClueWithProgress(Clue clue, ClueProgress progress) {
        this.clue = clue;
        this.progress = progress;
    }
}
