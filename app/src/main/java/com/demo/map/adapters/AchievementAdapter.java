package com.demo.map.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.demo.map.R;
import com.demo.map.model.Achievement;

import java.util.List;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.ViewHolder> {
    private final List<Achievement> achievements;
    private final Context context;

    public AchievementAdapter(Context context, List<Achievement> achievements) {
        this.context = context;
        this.achievements = achievements;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_achievement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Achievement achievement = achievements.get(position);
        
        holder.titleText.setText(achievement.getType().getTitle());
        holder.descriptionText.setText(achievement.getType().getDescription());
        
        if (achievement.isUnlocked()) {
            holder.progressBar.setVisibility(View.GONE);
            holder.progressText.setText(context.getString(R.string.achievement_unlocked));
            holder.pointsText.setText(String.format("+%d", achievement.getType().getPoints()));
            holder.itemView.setAlpha(1.0f);
        } else {
            holder.progressBar.setVisibility(View.VISIBLE);
            holder.progressBar.setProgress((int) achievement.getProgressPercentage());
            holder.progressText.setText(
                context.getString(R.string.achievement_progress, 
                (int) achievement.getProgressPercentage())
            );
            holder.pointsText.setText("");
            holder.itemView.setAlpha(0.6f);
        }
    }

    @Override
    public int getItemCount() {
        return achievements.size();
    }

    public void updateAchievements(List<Achievement> newAchievements) {
        achievements.clear();
        achievements.addAll(newAchievements);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView titleText;
        final TextView descriptionText;
        final TextView progressText;
        final TextView pointsText;
        final ProgressBar progressBar;

        ViewHolder(View view) {
            super(view);
            titleText = view.findViewById(R.id.achievementTitle);
            descriptionText = view.findViewById(R.id.achievementDescription);
            progressText = view.findViewById(R.id.achievementProgress);
            pointsText = view.findViewById(R.id.achievementPoints);
            progressBar = view.findViewById(R.id.achievementProgressBar);
        }
    }
} 