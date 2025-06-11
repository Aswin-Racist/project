package com.demo.map;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.demo.map.adapters.LeaderboardAdapter;
import com.demo.map.model.Player;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardActivity extends AppCompatActivity {
    private RecyclerView leaderboardRecyclerView;
    private LeaderboardAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        leaderboardRecyclerView = findViewById(R.id.leaderboardRecyclerView);
        leaderboardRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new LeaderboardAdapter(new ArrayList<>());
        leaderboardRecyclerView.setAdapter(adapter);

        loadLeaderboard();
    }

    private void loadLeaderboard() {
        // TODO: Load actual leaderboard data
        List<Player> players = new ArrayList<>();
        adapter.updatePlayers(players);
    }
} 