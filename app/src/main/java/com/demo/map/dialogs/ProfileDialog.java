package com.demo.map.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.demo.map.R;
import com.demo.map.adapters.AchievementAdapter;
import com.demo.map.model.Achievement;
import com.demo.map.model.Player;

import java.util.ArrayList;
import java.util.List;

public class ProfileDialog extends DialogFragment {
    private RecyclerView achievementsRecyclerView;
    private AchievementAdapter adapter;
    private Player player;

    public static ProfileDialog newInstance(Player player) {
        ProfileDialog dialog = new ProfileDialog();
        dialog.player = player;
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_profile, container, false);
        
        achievementsRecyclerView = view.findViewById(R.id.achievementsRecyclerView);
        achievementsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new AchievementAdapter(getContext(), new ArrayList<>());
        achievementsRecyclerView.setAdapter(adapter);
        
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        TextView nameText = view.findViewById(R.id.playerName);
        TextView scoreText = view.findViewById(R.id.playerScore);

        // Set player info
        if (player != null) {
            nameText.setText(player.getName());
            scoreText.setText(String.valueOf(player.getScore()));
        }
    }

    public void updateAchievements(List<Achievement> achievements) {
        if (adapter != null) {
            adapter.updateAchievements(achievements);
        }
    }
} 