package com.adventure.solo.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.adventure.solo.R;
import com.adventure.solo.model.PlayerProfile;
import com.adventure.solo.model.firebase.Team;
import java.util.ArrayList;
import java.util.HashMap; // Added for HashMap
import java.util.List;
import java.util.Map;

public class TeamAdminAdapter extends RecyclerView.Adapter<TeamAdminAdapter.TeamViewHolder> {

    private List<Team> teams = new ArrayList<>();
    private Map<String, List<PlayerProfile>> teamMembersMap = new HashMap<>();

    public void submitData(List<Team> newTeams, Map<String, List<PlayerProfile>> newTeamMembersMap) {
        this.teams = newTeams != null ? new ArrayList<>(newTeams) : new ArrayList<>(); // Defensive copy
        this.teamMembersMap = newTeamMembersMap != null ? new HashMap<>(newTeamMembersMap) : new HashMap<>(); // Defensive copy
        notifyDataSetChanged(); // Consider DiffUtil for better performance later
    }

    @NonNull
    @Override
    public TeamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_team_admin, parent, false);
        return new TeamViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeamViewHolder holder, int position) {
        Team team = teams.get(position);
        holder.bind(team, teamMembersMap.get(team.teamId));
    }

    @Override
    public int getItemCount() {
        return teams.size();
    }

    static class TeamViewHolder extends RecyclerView.ViewHolder {
        TextView teamNameTextView, teamIdTextView, teamLeaderTextView, teamMembersTextView;

        public TeamViewHolder(@NonNull View itemView) {
            super(itemView);
            teamNameTextView = itemView.findViewById(R.id.item_team_name_text_view);
            teamIdTextView = itemView.findViewById(R.id.item_team_id_text_view);
            teamLeaderTextView = itemView.findViewById(R.id.item_team_leader_text_view);
            teamMembersTextView = itemView.findViewById(R.id.item_team_members_text_view);
        }

        public void bind(Team team, List<PlayerProfile> members) {
            teamNameTextView.setText(team.teamName != null ? team.teamName : "N/A");
            teamIdTextView.setText("ID: " + (team.teamId != null ? team.teamId : "N/A"));

            String leaderDisplay = "N/A";
            if (team.teamLeaderPlayerId != null) {
                // Find leader's profile to display username if available
                if (members != null) {
                    for (PlayerProfile member : members) {
                        if (member.firebaseUid.equals(team.teamLeaderPlayerId)) {
                            leaderDisplay = member.username != null ? member.username : team.teamLeaderPlayerId.substring(0, Math.min(8, team.teamLeaderPlayerId.length())) + "...";
                            break;
                        }
                    }
                }
                if (leaderDisplay.equals("N/A")) { // Fallback if leader not in members list or members list is null
                     leaderDisplay = team.teamLeaderPlayerId.substring(0, Math.min(8, team.teamLeaderPlayerId.length())) + "...";
                }
            }
            teamLeaderTextView.setText("Leader: " + leaderDisplay);

            StringBuilder membersStr = new StringBuilder();
            if (members != null && !members.isEmpty()) {
                for (PlayerProfile member : members) {
                    membersStr.append("- ")
                              .append(member.username != null ? member.username : member.firebaseUid.substring(0, Math.min(8, member.firebaseUid.length()))+"...")
                              .append("\n");
                }
            } else if (team.memberPlayerIds != null && !team.memberPlayerIds.isEmpty()) {
                // Fallback if PlayerProfile objects are not available, show UIDs
                for (String memberId : team.memberPlayerIds) {
                     membersStr.append("- ")
                               .append(memberId.substring(0, Math.min(8, memberId.length()))+"...")
                               .append(" (UID)\n");
                }
            }
            else {
                membersStr.append("No members.");
            }
            teamMembersTextView.setText(membersStr.toString().trim());
        }
    }
}
