package com.example.damka;

import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity {
    private GameSessionManager gameSessionManager;
    private BoardGame boardGame; // Custom view for the game board
    private FrameLayout boardContainer; // Layout to hold the BoardGame

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Step 1: Initialize UI components
        boardContainer = findViewById(R.id.board_container);

        // Step 2: Retrieve intent extras
        String gameId = getIntent().getStringExtra("gameId");
        String currentPlayerId = getIntent().getStringExtra("playerId");
        boolean isPlayer1 = getIntent().getBooleanExtra("isPlayer1", false);

        // Step 3: Initialize GameSessionManager
        gameSessionManager = new GameSessionManager(gameId, currentPlayerId);

        // Step 4: Create or join a game
        if(isPlayer1)
            gameSessionManager.createGameSession(currentPlayerId);
        else
            gameSessionManager.joinGameSession(currentPlayerId);

        // Step 5: Initialize the BoardGame view
        boardGame = new BoardGame(this, gameSessionManager);
        boardContainer.addView(boardGame); // Add the BoardGame view to the layout

        // Step 5: Set up listener for game updates
        setupGameSessionListener();
    }

    private void setupGameSessionListener() {
        gameSessionManager.setOnGameSessionUpdateListener(new GameSessionManager.GameSessionUpdateListener() {
            @Override
            public void onPlayer1Updated(String player1Id) {
                Log.d("GameSession", "Player 1 updated: " + player1Id);
            }

            @Override
            public void onPlayer2Updated(String player2Id) {
                Log.d("GameSession", "Player 2 updated: " + player2Id);

                // Notify when Player 2 joins
                if (player2Id != null) {
                    Toast.makeText(GameActivity.this, "Player 2 has joined the game!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onTurnUpdated(String currentTurn) {
                if (boardGame != null) {
                    boardGame.setCurrentTurn(currentTurn); // Pass turn updates to BoardGame
                }
            }

            @Override
            public void onBoardStateUpdated(int[][] newState) {
                // Update the board state in BoardGame
                if (boardGame != null) {
                    boardGame.updateBoardState(newState);
                }
            }
        });
    }
}
