package com.adventure.solo.ui.inventory;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.DiffUtil; // For more efficient updates
import com.adventure.solo.R;
import com.adventure.solo.model.wrapper.QuestWithProgress;
import java.util.ArrayList;
import java.util.List;

public class CompletedQuestAdapter extends RecyclerView.Adapter<CompletedQuestAdapter.ViewHolder> {

    private List<QuestWithProgress> completedQuests = new ArrayList<>();

    public void submitList(List<QuestWithProgress> newQuests) {
        // For more complex lists and better performance, implement DiffUtil
        // Example:
        // final QuestDiffCallback diffCallback = new QuestDiffCallback(this.completedQuests, newQuests);
        // final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        // this.completedQuests.clear();
        // this.completedQuests.addAll(newQuests);
        // diffResult.dispatchUpdatesTo(this);

        this.completedQuests = newQuests != null ? new ArrayList<>(newQuests) : new ArrayList<>(); // Create new list
        notifyDataSetChanged(); // Simpler for now
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_completed_quest, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuestWithProgress qwp = completedQuests.get(position);
        holder.bind(qwp);
    }

    @Override
    public int getItemCount() {
        return completedQuests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, descriptionTextView, completedByTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.item_quest_title_text_view);
            descriptionTextView = itemView.findViewById(R.id.item_quest_description_text_view);
            completedByTextView = itemView.findViewById(R.id.item_quest_completed_by_text_view);
        }

        public void bind(QuestWithProgress qwp) {
            if (qwp.quest != null) {
                titleTextView.setText(qwp.quest.getTitle() != null ? qwp.quest.getTitle() : "N/A");
                descriptionTextView.setText(qwp.quest.getDescription() != null ? qwp.quest.getDescription() : "");
            } else {
                titleTextView.setText("Quest Data Missing");
                descriptionTextView.setText("");
            }
            if (qwp.progress != null && qwp.progress.lastCompletedByPlayerId != null) {
                // TODO: Fetch player username for lastCompletedByPlayerId if desired.
                // For now, show the ID or a generic message.
                String completerInfo = qwp.progress.lastCompletedByPlayerId;
                if (completerInfo.length() > 8) { // Shorten UID for display
                    completerInfo = completerInfo.substring(0, 8) + "...";
                }
                completedByTextView.setText("Completed by team (last by: " + completerInfo + ")");
            } else {
                completedByTextView.setText("Completed by team");
            }
        }
    }

    // Example DiffUtil.ItemCallback (for ListAdapter or manual DiffUtil usage)
    // public static class QuestDiffCallback extends DiffUtil.Callback {
    //    private final List<QuestWithProgress> oldList;
    //    private final List<QuestWithProgress> newList;
    //    public QuestDiffCallback(List<QuestWithProgress> oldList, List<QuestWithProgress> newList) {
    //        this.oldList = oldList;
    //        this.newList = newList;
    //    }
    //    @Override public int getOldListSize() { return oldList.size(); }
    //    @Override public int getNewListSize() { return newList.size(); }
    //    @Override public boolean areItemsTheSame(int oldPos, int newPos) {
    //        return oldList.get(oldPos).quest.getId() == newList.get(newPos).quest.getId();
    //    }
    //    @Override public boolean areContentsTheSame(int oldPos, int newPos) {
    //        // Implement more detailed content check if needed
    //        return oldList.get(oldPos).equals(newList.get(newPos));
    //    }
    // }
}
