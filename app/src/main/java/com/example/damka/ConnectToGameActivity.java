package com.example.damka;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.UUID;

public class ConnectToGameActivity extends AppCompatActivity implements View.OnClickListener {

    Button createGameButton, joinGameButton;
    AuthManager authManager;
    FireStoreManager firestoreManager;
    String gameId;
    String currentPlayerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect_to_game);

        createGameButton = findViewById(R.id.createGameButton);
        createGameButton.setOnClickListener(this);
        joinGameButton = findViewById(R.id.joinGameButton);
        joinGameButton.setOnClickListener(this);

        authManager = new AuthManager();
        firestoreManager = new FireStoreManager();
    }

    @Override
    public void onClick(View v) {
        if (v == createGameButton)
            createGame();
        if (v == joinGameButton)
            joinGame();
    }

    private void createGame() {
        currentPlayerId = authManager.getCurrentUserId();
        gameId = UUID.randomUUID().toString(); // Unique game ID
        boolean isPlayer1 = true;
        Log.d("DEBUG", "Successfully added game: " + gameId);
        startGameActivity(gameId, currentPlayerId, isPlayer1);
    }

    private void joinGame() {
        gameId = null;
        boolean isPlayer1 = false;
        currentPlayerId = authManager.getCurrentUserId();
        startGameActivity(gameId, currentPlayerId, isPlayer1);
    }

    private void startGameActivity(String gameId, String playerId, boolean isPlayer1) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("gameId", gameId);
        intent.putExtra("playerId", playerId);
        intent.putExtra("isPlayer1", isPlayer1); // true - player 1, false - player 2
        startActivity(intent);
    }
}