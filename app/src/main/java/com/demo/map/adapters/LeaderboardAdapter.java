package com.demo.map.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.demo.map.R;
import com.demo.map.model.Player;

import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {
    private List<Player> players;

    public LeaderboardAdapter(List<Player> players) {
        this.players = players;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Player player = players.get(position);
        holder.playerName.setText(player.getName());
        holder.playerScore.setText(String.valueOf(player.getScore()));
    }

    @Override
    public int getItemCount() {
        return players.size();
    }

    public void updatePlayers(List<Player> newPlayers) {
        this.players = newPlayers;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView playerName;
        TextView playerScore;

        ViewHolder(View itemView) {
            super(itemView);
            playerName = itemView.findViewById(R.id.playerName);
            playerScore = itemView.findViewById(R.id.playerScore);
        }
    }
} 