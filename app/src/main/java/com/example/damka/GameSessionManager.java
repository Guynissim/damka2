package com.example.damka;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameSessionManager {
    private final DatabaseReference gameRef;

    private String currentPlayerId, player1Id, player2Id, currentTurn;
    private int[][] boardState;
    private GameSessionUpdateListener gameSessionUpdateListener;

    public GameSessionManager(String gameId, String playerId) {
        if (gameId == null || gameId.isEmpty()) {
            throw new IllegalArgumentException("Game ID cannot be null or empty.");
        }

        this.currentPlayerId = playerId;
        this.gameRef = FirebaseDatabase.getInstance().getReference("GameSessions").child(gameId);

        Log.d("GameSessionManager", "Firebase reference initialized for game ID: " + gameId);
        listenForUpdates();
    }

    // Getters
    public String getPlayer1Id() {
        return player1Id;
    }

    public String getPlayer2Id() {
        return player2Id;
    }

    public String getCurrentPlayerId() {
        return currentPlayerId;
    }

    public String getCurrentTurn() {
        return currentTurn;
    }

    public int[][] getBoardState() {
        return boardState;
    }

    // Listener interface
    public interface GameSessionUpdateListener {
        void onPlayer1Updated(String player1Id);

        void onPlayer2Updated(String player2Id);

        void onTurnUpdated(String currentTurn);

        void onBoardStateUpdated(int[][] newState);
    }

    // Attach a listener
    public void setOnGameSessionUpdateListener(GameSessionUpdateListener listener) {
        this.gameSessionUpdateListener = listener;
    }

    // Create a new game session
    public void createGameSession(String playerId) {
        if (gameRef == null) {
            Log.e("GameSessionManager", "Game reference is null. Cannot create session.");
            return;
        }

        int[][] initialBoardState = getInitialBoardState();
        Map<String, Object> initialState = new HashMap<>();
        initialState.put("player1Id", playerId);
        initialState.put("player2Id", null);
        initialState.put("currentTurn", playerId);
        initialState.put("boardState", convertArrayToList(initialBoardState));
        initialState.put("createdAt", System.currentTimeMillis());

        // Local updates
        currentTurn = playerId;
        player1Id = playerId;
        boardState = initialBoardState;

        gameRef.setValue(initialState).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("GameSessionManager", "Game session created successfully.");
            } else {
                Log.e("GameSessionManager", "Failed to create game session.", task.getException());
            }
        });
    }


    // Join an existing game session
    public void joinGameSession(String playerId) {
        gameRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                if (mutableData.child("player2Id").getValue(String.class) == null) {
                    mutableData.child("player2Id").setValue(playerId);
                    return Transaction.success(mutableData);
                }
                return Transaction.abort(); // Player 2 already exists
            }

            @Override
            public void onComplete(@NonNull DatabaseError error, boolean committed, @NonNull DataSnapshot snapshot) {
                if (committed) {
                    Log.d("GameSessionManager", "Player 2 joined successfully.");
                } else {
                    Log.e("GameSessionManager", "Failed to join game session: " + error.getMessage());
                }
            }
        });
    }

    // Update the board state in Firebase
    public void updateBoardState(int[][] newState) {
        gameRef.child("boardState").setValue(convertArrayToList(newState)).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("GameSessionManager", "Board state updated successfully.");
            } else {
                Log.e("GameSessionManager", "Failed to update board state.", task.getException());
            }
        });
    }

    // Update the turn in Firebase
    public void updateTurn(String playerId) {
        gameRef.child("currentTurn").setValue(playerId).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("GameSessionManager", "Turn updated successfully.");
            } else {
                Log.e("GameSessionManager", "Failed to update turn.", task.getException());
            }
        });
    }

    // Listen for changes in Firebase
    private void listenForUpdates() {
        gameRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                player1Id = snapshot.child("player1Id").getValue(String.class);
                player2Id = snapshot.child("player2Id").getValue(String.class);
                currentTurn = snapshot.child("currentTurn").getValue(String.class);

                // Fetch and update boardState
                List<List<Long>> boardList = (List<List<Long>>) snapshot.child("boardState").getValue();
                if (boardList != null) {
                    boardState = convertListToArray(boardList);
                } else {
                    Log.e("GameSessionManager", "boardState is null in snapshot.");
                }

                // Notify listener safely
                if (gameSessionUpdateListener != null) {
                    gameSessionUpdateListener.onPlayer1Updated(player1Id);
                    gameSessionUpdateListener.onPlayer2Updated(player2Id);
                    gameSessionUpdateListener.onTurnUpdated(currentTurn);

                    if (boardState != null) {
                        gameSessionUpdateListener.onBoardStateUpdated(boardState);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("GameSessionManager", "Failed to listen for updates.", error.toException());
            }
        });
    }


    // Helper to get the initial board state
    private int[][] getInitialBoardState() {
        int[][] initialBoard = new int[8][8];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 8; j++) {
                if ((i + j) % 2 != 0) initialBoard[i][j] = 1;
            }
        }
        for (int i = 5; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if ((i + j) % 2 != 0) initialBoard[i][j] = 2;
            }
        }
        return initialBoard;
    }

    // Helper to convert Firebase List<List<Long>> to int[][]
    private int[][] convertListToArray(List<List<Long>> list) {
        int[][] array = new int[list.size()][];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i).stream().mapToInt(Long::intValue).toArray();
        }
        return array;
    }

    // Helper to convert int[][] to List<List<Integer>> for Firebase
    private List<List<Integer>> convertArrayToList(int[][] array) {
        List<List<Integer>> list = new ArrayList<>();
        for (int[] row : array) {
            List<Integer> rowList = new ArrayList<>();
            for (int cell : row) {
                rowList.add(cell);
            }
            list.add(rowList);
        }
        return list;
    }
}
