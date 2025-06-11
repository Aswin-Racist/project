package com.demo.map;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import com.demo.map.game.ScavengerHuntGame;
import com.demo.map.model.Player;

public class ScavengerHuntApplication extends Application {
    private static final String PREF_PLAYER_ID = "player_id";
    private static final String PREF_PLAYER_NAME = "player_name";
    private static final String PREF_PLAYER_AVATAR = "player_avatar";
    
    private ScavengerHuntGame game;
    private Player currentPlayer;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        game = new ScavengerHuntGame();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        // Load or create player
        String playerId = prefs.getString(PREF_PLAYER_ID, null);
        String playerName = prefs.getString(PREF_PLAYER_NAME, null);
        String playerAvatar = prefs.getString(PREF_PLAYER_AVATAR, null);
        
        if (playerId == null || playerName == null) {
            // New player will be created when avatar is selected
            currentPlayer = null;
        } else {
            currentPlayer = new Player(playerId, playerName);
            currentPlayer.setAvatarResource(playerAvatar);
            game.addPlayer(currentPlayer);
        }
    }

    public void createPlayer(String name, String avatarResource) {
        currentPlayer = new Player(String.valueOf(System.currentTimeMillis()), name);
        currentPlayer.setAvatarResource(avatarResource);
        game.addPlayer(currentPlayer);
        
        // Save player info
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_PLAYER_ID, currentPlayer.getId());
        editor.putString(PREF_PLAYER_NAME, currentPlayer.getName());
        editor.putString(PREF_PLAYER_AVATAR, avatarResource);
        editor.apply();
    }

    public ScavengerHuntGame getGame() {
        return game;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public boolean isPlayerCreated() {
        return currentPlayer != null;
    }
} 