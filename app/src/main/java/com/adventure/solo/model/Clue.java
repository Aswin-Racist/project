package com.adventure.solo.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters; // For ClueType converter
import com.adventure.solo.database.Converters; // Assuming Converters class location

@Entity(tableName = "clues",
        foreignKeys = @ForeignKey(entity = Quest.class,
                parentColumns = "id",
                childColumns = "questId",
                onDelete = ForeignKey.CASCADE))
@TypeConverters(Converters.class) // Added for ClueType
public class Clue {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long questId;
    private String text;
    private double targetLatitude;
    private double targetLongitude;
    private int sequenceNumber;
    private boolean discovered; // This 'discovered' might become legacy if ClueProgress handles all state
    private String type; // Legacy 'type' field, can be deprecated or mapped from ClueType
    private String hint;

    public ClueType clueType; // New field
    public String puzzleData; // New field: e.g., "Riddle|Answer" or "Num1|Op|Num2|Answer"

    // Original constructor - updated to initialize new fields
    public Clue(long questId, String text, double targetLatitude, double targetLongitude,
                int sequenceNumber, String type, String hint) {
        this.questId = questId;
        this.text = text;
        this.targetLatitude = targetLatitude;
        this.targetLongitude = targetLongitude;
        this.sequenceNumber = sequenceNumber;
        this.type = type; // Legacy
        this.hint = hint;
        this.discovered = false; // Legacy default
        // Attempt to map legacy type to new ClueType, default to LOCATION
        try {
            if (type != null && !type.isEmpty()) {
                this.clueType = ClueType.valueOf(type.toUpperCase());
            } else {
                this.clueType = ClueType.LOCATION;
            }
        } catch (IllegalArgumentException e) {
            this.clueType = ClueType.LOCATION; // Default if legacy type is not a valid ClueType
        }
        this.puzzleData = null; // No puzzle data for old constructor
    }

    // New constructor for all fields including puzzle type
    public Clue(long questId, String text, double targetLatitude, double targetLongitude,
                int sequenceNumber, String hint, ClueType clueType, String puzzleData) {
        this.questId = questId;
        this.text = text; // Riddle text or math problem
        this.targetLatitude = targetLatitude; // Location where puzzle is active or AR appears after solve
        this.targetLongitude = targetLongitude;
        this.sequenceNumber = sequenceNumber;
        this.hint = hint;
        this.clueType = clueType;
        this.puzzleData = puzzleData;
        this.type = clueType != null ? clueType.name() : ClueType.LOCATION.name(); // Sync legacy type field
        this.discovered = false; // Default state
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
    public ClueType getClueType() { return clueType; }
    public void setClueType(ClueType clueType) { this.clueType = clueType; }
    public String getPuzzleData() { return puzzleData; }
    public void setPuzzleData(String puzzleData) { this.puzzleData = puzzleData; }
} 