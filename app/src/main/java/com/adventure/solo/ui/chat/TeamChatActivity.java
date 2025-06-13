package com.adventure.solo.ui.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.adventure.solo.databinding.ActivityTeamChatBinding;
import com.adventure.solo.model.PlayerProfile;
import com.adventure.solo.model.firebase.ChatMessage;
import com.adventure.solo.repository.PlayerProfileRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;
import android.util.Log;


@AndroidEntryPoint
public class TeamChatActivity extends AppCompatActivity {
    private static final String TAG = "TeamChatActivity";
    public static final String EXTRA_TEAM_ID = "extra_team_id"; // Key for passing teamId via Intent

    private ActivityTeamChatBinding binding;
    private ChatMessageAdapter adapter;
    private DatabaseReference chatMessagesRef;
    private ChildEventListener childEventListener;

    private String teamIdFromIntent; // Store teamId passed via intent
    private FirebaseUser currentUser;
    private String currentUsername; // Display name for the current user

    @Inject PlayerProfileRepository playerProfileRepository;
    @Inject FirebaseAuth firebaseAuth; // Injected via Hilt (FirebaseModule)
    @Inject FirebaseDatabase firebaseDatabase; // Injected via Hilt (FirebaseModule)


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTeamChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        currentUser = firebaseAuth.getCurrentUser();
        teamIdFromIntent = getIntent().getStringExtra(EXTRA_TEAM_ID);

        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to chat.", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if user not logged in
            return;
        }

        // Fetch PlayerProfile to get username and to verify/get teamId if not passed via intent
        playerProfileRepository.getPlayerProfile(currentUser.getUid(), profile -> {
            if (profile != null) {
                currentUsername = (profile.username != null && !profile.username.isEmpty()) ? profile.username : currentUser.getDisplayName();
                // If teamId was NOT passed via intent, use the one from the profile
                if (TextUtils.isEmpty(teamIdFromIntent)) {
                    teamIdFromIntent = profile.teamId;
                } else {
                    // If teamId WAS passed via intent, verify it matches the profile's teamId
                    // This prevents a user from trying to access another team's chat via a crafted intent
                    if (!teamIdFromIntent.equals(profile.teamId)) {
                        Log.w(TAG, "Intent teamId (" + teamIdFromIntent + ") does not match profile teamId (" + profile.teamId + "). Access denied.");
                        showAccessDenied("Team ID mismatch or you're not in this team.");
                        return;
                    }
                }
            } else {
                 // No local profile, use Firebase display name if available
                 currentUsername = currentUser.getDisplayName();
                 if (TextUtils.isEmpty(currentUsername)) {
                     currentUsername = currentUser.getEmail(); // Fallback to email if display name is also empty
                 }
                 // If teamId was not passed in intent and no profile, user cannot chat
                 if (TextUtils.isEmpty(teamIdFromIntent)) {
                     Log.e(TAG, "No local profile found and no teamId passed in intent.");
                     showAccessDenied("Player profile not found and no team specified.");
                     return;
                 }
            }

            // Final check for teamId after potentially getting it from profile
            if (TextUtils.isEmpty(teamIdFromIntent)) {
                showAccessDenied("You are not part of a team or team ID is missing.");
                return;
            }

            // If all checks pass:
            binding.noTeamChatTextView.setVisibility(View.GONE);
            binding.sendMessageLayout.setVisibility(View.VISIBLE);
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
            setupChat(teamIdFromIntent);
        });
    }

    private void showAccessDenied(String message) {
        binding.chatRecyclerView.setVisibility(View.GONE);
        binding.sendMessageLayout.setVisibility(View.GONE);
        binding.noTeamChatTextView.setText(message);
        binding.noTeamChatTextView.setVisibility(View.VISIBLE);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        // Optionally finish(); or disable send button
        binding.sendMessageButton.setEnabled(false);
    }

    private void setupChat(String resolvedTeamId) {
        this.teamId = resolvedTeamId; // Set the class field
        setTitle("Team Chat: " + teamId.substring(0, Math.min(teamId.length(), 6))+"..."); // Shorten for title

        String uidForAdapter = (currentUser != null && currentUser.getUid() != null) ? currentUser.getUid() : "UNKNOWN_UID";
        if (uidForAdapter.equals("UNKNOWN_UID")) {
            Log.e(TAG, "Cannot initialize ChatMessageAdapter with a valid currentUserId.");
            // Potentially show error and finish activity as chat won't work correctly
            Toast.makeText(this, "Error: User session invalid for chat.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        adapter = new ChatMessageAdapter(uidForAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // New messages appear at the bottom and scroll
        binding.chatRecyclerView.setLayoutManager(layoutManager);
        binding.chatRecyclerView.setAdapter(adapter);

        // Path: /chats/{teamId}/messages
        chatMessagesRef = firebaseDatabase.getReference("chats").child(teamId).child("messages");

        binding.sendMessageButton.setOnClickListener(v -> sendMessage());
        listenForMessages();
    }


    private void sendMessage() {
        String messageText = binding.messageEditText.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) {
            return;
        }
        // Ensure currentUser and teamId are valid before sending
        if (currentUser == null || TextUtils.isEmpty(teamId)) {
             Toast.makeText(this, "Cannot send message. User or Team ID missing.", Toast.LENGTH_SHORT).show();
             return;
        }

        String senderDisplayName = (currentUsername != null && !currentUsername.isEmpty()) ? currentUsername :
                                   (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty() ? currentUser.getDisplayName() : currentUser.getEmail());

        ChatMessage chatMessage = new ChatMessage(teamId, currentUser.getUid(), senderDisplayName, messageText);

        // Firebase will generate a unique key for the message
        DatabaseReference newMsgRef = chatMessagesRef.push();
        chatMessage.messageId = newMsgRef.getKey(); // Store the generated key in the message object

        newMsgRef.setValue(chatMessage)
            .addOnSuccessListener(aVoid -> {
                binding.messageEditText.setText("");
                Log.d(TAG, "Message sent successfully: " + chatMessage.messageId);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to send message", e);
                Toast.makeText(TeamChatActivity.this, "Failed to send message.", Toast.LENGTH_SHORT).show();
            });
    }

    private void listenForMessages() {
        if (chatMessagesRef == null) {
            Log.e(TAG, "chatMessagesRef is null, cannot listen for messages.");
            return;
        }
        if (childEventListener != null) { // Remove previous listener if any (e.g., if teamId changed)
             chatMessagesRef.removeEventListener(childEventListener);
        }
        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                try {
                    ChatMessage message = snapshot.getValue(ChatMessage.class);
                    if (message != null) {
                        message.messageId = snapshot.getKey();
                        adapter.addMessage(message);
                        binding.chatRecyclerView.scrollToPosition(adapter.getItemCount() - 1);
                    } else {
                        Log.w(TAG, "Received null message object from Firebase snapshot.");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error deserializing chat message", e);
                }
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) { /* Handle edited messages if feature exists */ }
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) { /* Handle deleted messages if feature exists */ }
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) { /* Not typical for chat */ }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Chat listener cancelled", error.toException());
                Toast.makeText(TeamChatActivity.this, "Chat listener error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        chatMessagesRef.addChildEventListener(childEventListener);
        Log.d(TAG, "Attached chat message listener for team: " + teamId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Removing chat listener.");
        if (childEventListener != null && chatMessagesRef != null) {
            chatMessagesRef.removeEventListener(childEventListener);
        }
        binding = null; // For ViewBinding
    }
}
