package com.demo.map.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.demo.map.R;
import com.google.android.material.button.MaterialButton;

public class AvatarSelectionDialog extends DialogFragment {
    private OnAvatarSelectedListener listener;
    private String selectedAvatar = null;
    private EditText nameInput;

    public interface OnAvatarSelectedListener {
        void onAvatarSelected(String name, String avatarResource);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (OnAvatarSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnAvatarSelectedListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_avatar_selection, null);

        // Add name input at the top
        nameInput = new EditText(requireContext());
        nameInput.setHint(R.string.enter_name);
        nameInput.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        ((LinearLayout) view).addView(nameInput, 0);

        // Setup avatar click listeners
        setupAvatarClickListener(view, R.id.whiteboy_avatar, "whiteboy");
        setupAvatarClickListener(view, R.id.blackboy_avatar, "blackboy");
        setupAvatarClickListener(view, R.id.whitegirl_avatar, "whitegirl");
        setupAvatarClickListener(view, R.id.blackgirl_avatar, "blackgirl");

        // Add confirm button at the bottom
        MaterialButton confirmButton = new MaterialButton(requireContext());
        confirmButton.setText(R.string.confirm);
        confirmButton.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        ((LinearLayout) view).addView(confirmButton);

        confirmButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            if (!name.isEmpty() && selectedAvatar != null) {
                listener.onAvatarSelected(name, selectedAvatar);
                dismiss();
            }
        });

        builder.setView(view)
               .setTitle(R.string.select_avatar)
               .setNegativeButton(R.string.cancel, (dialog, id) -> dismiss());

        return builder.create();
    }

    private void setupAvatarClickListener(View rootView, int avatarViewId, String avatarResource) {
        ImageView avatarView = rootView.findViewById(avatarViewId);
        avatarView.setOnClickListener(v -> {
            // Clear previous selection
            clearSelections(rootView);
            // Set new selection
            v.setBackgroundResource(R.drawable.selected_avatar_background);
            selectedAvatar = avatarResource;
        });
    }

    private void clearSelections(View rootView) {
        ((ImageView) rootView.findViewById(R.id.whiteboy_avatar)).setBackground(null);
        ((ImageView) rootView.findViewById(R.id.blackboy_avatar)).setBackground(null);
        ((ImageView) rootView.findViewById(R.id.whitegirl_avatar)).setBackground(null);
        ((ImageView) rootView.findViewById(R.id.blackgirl_avatar)).setBackground(null);
    }
} 