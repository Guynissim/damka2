package com.example.damka;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConnectToGameActivity extends AppCompatActivity implements View.OnClickListener {

    Button createGameButton, joinGameButton, joinRanGameButton;
    EditText joinGameEditText;
    GameSessionManager gameSessionManager;
    AuthManager authManager;
    FireStoreManager firestoreManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_to_game);

        createGameButton = findViewById(R.id.createGameButton);
        createGameButton.setOnClickListener(this);
        joinRanGameButton = findViewById(R.id.joinRanGameButton);
        joinRanGameButton.setOnClickListener(this);
        joinGameButton = findViewById(R.id.joinGameButton);
        joinGameButton.setOnClickListener(this);
        joinGameEditText = findViewById(R.id.joinGameEditText);

        authManager = new AuthManager();
        firestoreManager = new FireStoreManager();
    }

    @Override
    public void onClick(View v) {
        if (v == createGameButton)
            createGame();
        if (v == joinGameButton)
            joinSpecificGame();
        if (v == joinRanGameButton)
            joinRanGame();
    }

    private void createGame() {
        String currentPlayerId = authManager.getCurrentUserId();
        String gameId = UUID.randomUUID().toString(); // Unique game ID
        gameSessionManager = new GameSessionManager(gameId, currentPlayerId);
        Map<String, Object> gameData = new HashMap<>();
        gameData.put("gameId", gameId);
        gameData.put("createdAt", System.currentTimeMillis());

        firestoreManager.addGameToWaitingList(gameId, gameData, task -> {
            if (task.isSuccessful()) {
                boolean isPlayer1 = true;
                Log.d("DEBUG", "Successfully added game: " + gameId);
                gameSessionManager.createGameSession(currentPlayerId);
                startGameActivity(gameId, currentPlayerId, isPlayer1);
            } else {
                Log.e("DEBUG", "Failed to create game.");
            }
        });
    }

    private void joinSpecificGame() {
        String gameId = joinGameEditText.getText().toString().trim(); // Trim for clean input
        String currentPlayerId = authManager.getCurrentUserId();
        boolean isPlayer1 = false;

        // Initialize GameSessionManager and join the session
        GameSessionManager gameSessionManager = new GameSessionManager(gameId, currentPlayerId);
        gameSessionManager.joinGameSession(currentPlayerId);

        // Start GameActivity
        startGameActivity(gameId, currentPlayerId, isPlayer1);
    }

    private void joinRanGame() {
        firestoreManager.getWaitingGame(task -> {
            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                boolean isPlayer1 = false;
                String gameId = task.getResult().getDocuments().get(0).getId();
                String currentPlayerId = authManager.getCurrentUserId();
                gameSessionManager = new GameSessionManager(gameId, currentPlayerId);
                gameSessionManager.joinGameSession(currentPlayerId);
                startGameActivity(gameId, currentPlayerId, isPlayer1);
            } else {
                Toast.makeText(this, "No random games available.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startGameActivity(String gameId, String playerId, boolean isPlayer1) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("gameId", gameId);
        intent.putExtra("playerId", playerId);
        intent.putExtra("isPlayer1", isPlayer1); // true - player 1, false - player 2
        startActivity(intent);
    }
}