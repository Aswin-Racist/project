package com.adventure.solo.ui.puzzle;

import android.app.Dialog;
import android.content.Context; // Required for listener
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.adventure.solo.databinding.DialogPuzzleDisplayBinding;
import com.adventure.solo.model.ClueType;
import android.util.Log;

public class PuzzleDisplayFragment extends DialogFragment {

    public static final String TAG = "PuzzleDisplayFragment";
    private static final String ARG_CLUE_ID = "clue_id";
    private static final String ARG_CLUE_TYPE_NAME = "clue_type_name"; // Storing enum name as String
    private static final String ARG_PUZZLE_TEXT = "puzzle_text";
    private static final String ARG_PUZZLE_ANSWER = "puzzle_answer";

    public interface PuzzleSolvedListener {
        void onPuzzleSolved(long clueId);
    }
    private PuzzleSolvedListener listener;

    private DialogPuzzleDisplayBinding binding;
    private long clueId;
    private String correctAnswer;
    // private ClueType clueType; // Can be re-derived from name if needed

    public static PuzzleDisplayFragment newInstance(long clueId, ClueType clueTypeEnum, String puzzleFullData) {
        PuzzleDisplayFragment fragment = new PuzzleDisplayFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_CLUE_ID, clueId);
        args.putString(ARG_CLUE_TYPE_NAME, clueTypeEnum.name()); // Store enum name

        if (puzzleFullData == null || puzzleFullData.isEmpty()) {
            Log.e(TAG, "newInstance: puzzleFullData is null or empty for clueId " + clueId);
            args.putString(ARG_PUZZLE_TEXT, "Error: Puzzle data missing.");
            args.putString(ARG_PUZZLE_ANSWER, ""); // Invalid answer to prevent accidental solve
            fragment.setArguments(args);
            return fragment;
        }

        String[] parts = puzzleFullData.split("\\|"); // Corrected regex for literal '|'

        if (clueTypeEnum == ClueType.PUZZLE_TEXT_RIDDLE) {
            if (parts.length >= 2) {
                args.putString(ARG_PUZZLE_TEXT, parts[0]); // Riddle text
                args.putString(ARG_PUZZLE_ANSWER, parts[1]); // Answer
            } else {
                Log.e(TAG, "newInstance: Invalid PUZZLE_TEXT_RIDDLE data format: " + puzzleFullData);
                args.putString(ARG_PUZZLE_TEXT, "Error: Riddle data format incorrect.");
                args.putString(ARG_PUZZLE_ANSWER, "");
            }
        } else if (clueTypeEnum == ClueType.PUZZLE_MATH_SIMPLE) {
            if (parts.length >= 4) {
                // e.g., "5|ADD|3|8"  => "Solve: 5 + 3"
                String question = "Solve: " + parts[0] + " " + getMathSymbol(parts[1]) + " " + parts[2];
                args.putString(ARG_PUZZLE_TEXT, question);
                args.putString(ARG_PUZZLE_ANSWER, parts[3]); // Answer
            } else {
                Log.e(TAG, "newInstance: Invalid PUZZLE_MATH_SIMPLE data format: " + puzzleFullData);
                args.putString(ARG_PUZZLE_TEXT, "Error: Math puzzle data format incorrect.");
                args.putString(ARG_PUZZLE_ANSWER, "");
            }
        } else {
            Log.w(TAG, "newInstance: Unsupported ClueType for puzzle: " + clueTypeEnum);
            args.putString(ARG_PUZZLE_TEXT, "Error: Unknown puzzle type.");
            args.putString(ARG_PUZZLE_ANSWER, "");
        }
        fragment.setArguments(args);
        return fragment;
    }

    private static String getMathSymbol(String operatorName) {
        if (operatorName == null) return "?";
        switch (operatorName.toUpperCase()) {
            case "ADD": return "+";
            case "SUBTRACT": return "-";
            case "MULTIPLY": return "*";
            // Add other cases like DIVIDE if needed
            default: return "?";
        }
    }

    // Call this from the hosting Fragment/Activity to set the listener
    public void setPuzzleSolvedListener(PuzzleSolvedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Automatically try to set listener from parent Fragment or hosting Activity
        if (getParentFragment() instanceof PuzzleSolvedListener) {
            this.listener = (PuzzleSolvedListener) getParentFragment();
        } else if (context instanceof PuzzleSolvedListener) {
            // This might not be directly useful if shown via getParentFragmentManager()
            // but kept for flexibility if DialogFragment is shown differently.
            // this.listener = (PuzzleSolvedListener) context;
        }
        // If listener is still null here, it must be set via setPuzzleSolvedListener by the caller.
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogPuzzleDisplayBinding.inflate(LayoutInflater.from(getContext()));
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(binding.getRoot());

        if (getArguments() == null) {
            Log.e(TAG, "onCreateDialog: No arguments provided to PuzzleDisplayFragment.");
            // Handle error, perhaps dismiss or show error message
            binding.puzzleQuestionTextView.setText("Error: Missing puzzle data.");
            binding.submitPuzzleAnswerButton.setEnabled(false);
            return builder.create(); // Or throw exception
        }

        clueId = getArguments().getLong(ARG_CLUE_ID);
        String puzzleText = getArguments().getString(ARG_PUZZLE_TEXT, "Error: Puzzle text not found.");
        correctAnswer = getArguments().getString(ARG_PUZZLE_ANSWER, ""); // Empty answer if not found
        String clueTypeName = getArguments().getString(ARG_CLUE_TYPE_NAME);
        ClueType currentClueType = clueTypeName != null ? ClueType.valueOf(clueTypeName) : null;


        binding.puzzleQuestionTextView.setText(puzzleText);
        if (currentClueType == ClueType.PUZZLE_TEXT_RIDDLE) {
            binding.puzzleTitleTextView.setText("Riddle Challenge");
        } else if (currentClueType == ClueType.PUZZLE_MATH_SIMPLE) {
             binding.puzzleTitleTextView.setText("Math Challenge");
        } else {
            binding.puzzleTitleTextView.setText("Puzzle Time!");
        }


        binding.submitPuzzleAnswerButton.setOnClickListener(v -> {
            if (correctAnswer == null || correctAnswer.isEmpty()) {
                 Toast.makeText(getContext(), "Error: Puzzle answer not set.", Toast.LENGTH_SHORT).show();
                 return;
            }
            String userAnswer = binding.puzzleAnswerEditText.getText().toString().trim();
            if (userAnswer.equalsIgnoreCase(correctAnswer)) {
                Toast.makeText(getContext(), "Correct!", Toast.LENGTH_SHORT).show();
                if (listener != null) {
                    listener.onPuzzleSolved(clueId);
                } else {
                    Log.w(TAG, "PuzzleSolvedListener is null. Cannot report puzzle solved for clueId: " + clueId);
                }
                dismiss();
            } else {
                Toast.makeText(getContext(), "Incorrect. Try again!", Toast.LENGTH_SHORT).show();
                binding.puzzleAnswerEditText.setText("");
            }
        });

        // setCancelable(false); // Optional: prevent dismissal on outside touch

        return builder.create();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
