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
import com.demo.map.adapters.LeaderboardAdapter;
import com.demo.map.model.Player;

import java.util.ArrayList;
import java.util.List;

public class DashboardDialog extends DialogFragment {
    private RecyclerView leaderboardRecyclerView;
    private LeaderboardAdapter adapter;
    private TextView nearbyPlayersCount;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_dashboard, container, false);
        
        nearbyPlayersCount = view.findViewById(R.id.nearbyPlayersCount);
        leaderboardRecyclerView = view.findViewById(R.id.leaderboardRecyclerView);
        leaderboardRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new LeaderboardAdapter(new ArrayList<>());
        leaderboardRecyclerView.setAdapter(adapter);
        
        return view;
    }

    public void updateStats() {
        // This method will be called to update the dashboard stats
    }

    public void updateLeaderboard(List<Player> players) {
        if (adapter != null) {
            adapter.updatePlayers(players);
        }
        if (nearbyPlayersCount != null) {
            nearbyPlayersCount.setText(getString(R.string.nearby_players, players.size()));
        }
    }
} 