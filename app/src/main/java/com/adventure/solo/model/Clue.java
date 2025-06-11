package com.adventure.solo.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "clues",
        foreignKeys = @ForeignKey(entity = Quest.class,
                parentColumns = "id",
                childColumns = "questId",
                onDelete = ForeignKey.CASCADE))
public class Clue {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long questId;
    private String text;
    private double targetLatitude;
    private double targetLongitude;
    private int sequenceNumber;
    private boolean discovered;
    private String type; // "LOCATION", "RIDDLE", "TASK"
    private String hint;

    public Clue(long questId, String text, double targetLatitude, double targetLongitude,
                int sequenceNumber, String type, String hint) {
        this.questId = questId;
        this.text = text;
        this.targetLatitude = targetLatitude;
        this.targetLongitude = targetLongitude;
        this.sequenceNumber = sequenceNumber;
        this.type = type;
        this.hint = hint;
        this.discovered = false;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getQuestId() { return questId; }
    public void setQuestId(long questId) { this.questId = questId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public double getTargetLatitude() { return targetLatitude; }
    public void setTargetLatitude(double targetLatitude) { this.targetLatitude = targetLatitude; }

    public double getTargetLongitude() { return targetLongitude; }
    public void setTargetLongitude(double targetLongitude) { this.targetLongitude = targetLongitude; }

    public int getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(int sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public boolean isDiscovered() { return discovered; }
    public void setDiscovered(boolean discovered) { this.discovered = discovered; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getHint() { return hint; }
    public void setHint(String hint) { this.hint = hint; }
} 