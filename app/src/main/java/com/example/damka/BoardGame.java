package com.example.damka;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.List;

public class BoardGame extends View {
    private static final int NUM_OF_SQUARES = 8;
    private Square[][] squares;
    private Soldier selectedSoldier = null;
    private boolean isSoldierJumped = false;//checks if jump
    private int[][] boardState;
    private String currentTurn;
    private GameSessionManager gameSessionManager;

    public BoardGame(Context context, GameSessionManager gameSessionManager) {
        super(context);

        this.gameSessionManager = gameSessionManager;
        squares = new Square[NUM_OF_SQUARES][NUM_OF_SQUARES];

        // Fetch initial state
        boardState = gameSessionManager.getBoardState();
        currentTurn = gameSessionManager.getCurrentTurn();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Initialize the board layout
        int w = canvas.getWidth() / NUM_OF_SQUARES; // Width of each square
        int h = w;
        initBoard(w, h);

        if (boardState != null)
            getSquaresArrFromBoardState(w, h);

        // Draw the board and soldiers
        drawBoard(canvas);
    }

    private void initBoard(int w, int h) {
        int x = 0;
        int y = 500; // Example offset for the board
        int color;

        for (int i = 0; i < NUM_OF_SQUARES; i++) {
            for (int j = 0; j < NUM_OF_SQUARES; j++) {
                if (i % 2 == 0) {
                    color = (j % 2 == 0) ? Color.argb(175, 150, 75, 0) : Color.BLACK;
                } else {
                    color = (j % 2 == 0) ? Color.BLACK : Color.argb(175, 150, 75, 0);
                }

                squares[i][j] = new Square(x, y, color, w, h, i, j);
                x += w;
            }
            y += h;
            x = 0;
        }
    }

    private void getSquaresArrFromBoardState(int w, int h) {
        for (int i = 0; i < NUM_OF_SQUARES; i++) {
            for (int j = 0; j < NUM_OF_SQUARES; j++) {
                int state = boardState[i][j];

                // Assign soldiers based on the board state
                if (state == 0) {
                    squares[i][j].soldier = null; // No soldier
                } else if (state == 1) {
                    squares[i][j].soldier = new Soldier(w / 2 + squares[i][j].x, h / 2 + squares[i][j].y, Color.RED, w / 3, i, j, 1);
                } else if (state == 2) {
                    squares[i][j].soldier = new Soldier(w / 2 + squares[i][j].x, h / 2 + squares[i][j].y, Color.BLUE, w / 3, i, j, 2);
                } else if (state == 3) {
                    squares[i][j].soldier = new King(w / 2 + squares[i][j].x, h / 2 + squares[i][j].y, Color.RED, w / 3, i, j, 1);
                } else if (state == 4) {
                    squares[i][j].soldier = new King(w / 2 + squares[i][j].x, h / 2 + squares[i][j].y, Color.BLUE, w / 3, i, j, 2);
                }
            }
        }
    }

    private void drawBoard(Canvas canvas) {
        for (int i = 0; i < NUM_OF_SQUARES; i++) {
            for (int j = 0; j < NUM_OF_SQUARES; j++) {
                squares[i][j].draw(canvas);
                if (squares[i][j].soldier != null) {
                    squares[i][j].soldier.draw(canvas);
                }
            }
        }
    }

    public void handleMove() {
        boardState = getBoardStateAfterMove();
        gameSessionManager.updateBoardState(boardState);
    }

    private int[][] getBoardStateAfterMove() {
        int[][] updatedBoardState = new int[NUM_OF_SQUARES][NUM_OF_SQUARES];
        for (int i = 0; i < NUM_OF_SQUARES; i++) {
            for (int j = 0; j < NUM_OF_SQUARES; j++) {
                updatedBoardState[i][j] = squares[i][j].getState();
            }
        }
        return updatedBoardState;
    }
    public void updateBoardState(int[][] newState) {
        boardState = newState;
    }

    public void setBoardState(List<List<Long>> boardStateList) {
        boardState = new int[boardStateList.size()][];
        for (int i = 0; i < boardStateList.size(); i++) {
            boardState[i] = boardStateList.get(i).stream().mapToInt(Long::intValue).toArray();
        }
        invalidate();
    }

    public void setCurrentTurn(String turn) {
        currentTurn = turn;
        updateTurnUI(turn);
    }

    public void updateTurnUI(String turn) {
        Toast.makeText(getContext(), "Current Turn: " + turn, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();
        int action = event.getAction();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                for (int i = 0; i < NUM_OF_SQUARES; i++) {
                    for (int j = 0; j < NUM_OF_SQUARES; j++) {
                        Square square = squares[i][j];
                        if (square.didUserTouchMe((int) touchX, (int) touchY) && square.soldier != null) {
                            selectedSoldier = square.soldier; // Select soldier for movement
                            invalidate();
                            Log.d("ACTION_DOWN", "Selected soldier at: " + i + ", " + j);
                            return true; // Stop further loop iterations
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (selectedSoldier != null) {
                    // Update the soldier's position while dragging and force redraw
                    selectedSoldier.Move((int) touchX, (int) touchY);
                    invalidate(); // Redraw the canvas
                    Log.d("ACTION_MOVE", "Dragging soldier to: " + touchX + ", " + touchY);
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
                if (selectedSoldier != null) {
                    updateColumnAndRow(selectedSoldier); // Update soldier's current position
                    if (!isValidSquare(selectedSoldier)) {
                        Log.d("Snap Failure", "No valid square found. Returning soldier to original position.");
                        // If snap fails, return soldier to its last position
                        selectedSoldier.Move(selectedSoldier.lastX, selectedSoldier.lastY);
                    } else {
                        Log.d("Snap Success", "Soldier snapped to a valid square.");
                    }
                    invalidate(); // Redraw the canvas
                    selectedSoldier = null; // Clear the selected soldier after placement
                    Log.d("ACTION_UP", "Released soldier.");
                    return true;
                }
                break;

            default:
                break;
        }
        return true;
    }

    private boolean isValidSquare(Soldier soldier) {
        if (gameSessionManager.getPlayer1Id() == null || gameSessionManager.getPlayer2Id() == null) {
            Toast.makeText(getContext(), "Waiting for Player 2 to join...", Toast.LENGTH_SHORT).show();
            return false;
        }
        King king;
        for (int i = 0; i < NUM_OF_SQUARES; i++) {

            for (int j = 0; j < NUM_OF_SQUARES; j++) {
                Square square = squares[i][j];
                if (square.didUserTouchMe(soldier.x, soldier.y) && square.soldier == null && square.color == Color.BLACK) {
                    Log.d("Square Check", "Square checked: column=" + j + ", row=" + i);
                    Log.d("Soldier Before", "Soldier: column=" + soldier.column + ", row=" + soldier.row);
                    isSoldierJumped = false;
                    if (soldier instanceof King) {
                        king = (King) soldier;
                        if (isValidMove(king)) {
                            king.Move(square.x + square.width / 2, square.y + square.height / 2);
                            square.soldier = king;
                            squares[king.lastColumn][king.lastRow].soldier = null;
                            updateLastPosition(king);
                            Log.d("Snap Success", "King snapped to valid square: " + square.x + ", " + square.y);
                            handleMove();
                            invalidate();
                            if (isSoldierJumped) {
                                displyWinner();
                            }
                            return true;
                        }
                    } else if (isValidMove(soldier)) {
                        soldier.Move(square.x + square.width / 2, square.y + square.height / 2);
                        squares[soldier.lastColumn][soldier.lastRow].soldier = null;
                        updateLastPosition(soldier);

                        if (soldier.side == 1 && soldier.column == 7 || soldier.side == 2 && soldier.column == 0) {
                            king = becomeKing(soldier);
                            square.soldier = king;
                        } else
                            square.soldier = soldier;
                        Log.d("Snap Success", "Soldier snapped to valid square: " + square.x + ", " + square.y);
                        handleMove();
                        invalidate();
                        if (isSoldierJumped) {
                            int winnerside = isGameOver();
                            if (winnerside == 1)
                                Toast.makeText(getContext(), "The winner side is BLUE!!!", Toast.LENGTH_SHORT).show();
                            if (winnerside == 2)
                                Toast.makeText(getContext(), "The winner side is Red!!!", Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private King becomeKing(Soldier soldier) {
        int x = soldier.x;
        int y = soldier.y;
        int side = soldier.side;
        int column = soldier.column;
        int row = soldier.row;
        int color = soldier.color;
        int radius = soldier.radius;
        King king = new King(x, y, color, radius, column, row, side);
        return king;
    }

    public boolean isValidMove(King king) {
        int columnStep = Math.abs(king.column - king.lastColumn);
        int rowStep = Math.abs(king.row - king.lastRow);
        Log.d("KingMove", "Validating king move: columnStep = " + columnStep + ", rowStep = " + rowStep);
        // Ensure the movement is diagonal and involves at least one step
        if (columnStep != rowStep || columnStep == 0) {
            Log.d("KingMove", "Invalid move: not diagonal or no movement.");
            return false;
        }
        int colDirection = (king.column - king.lastColumn) > 0 ? 1 : -1; // 1: Down, -1: Up
        int rowDirection = (king.row - king.lastRow) > 0 ? 1 : -1; // 1: Right, -1: Left
        boolean hasJumped = false; // Track if a jump has occurred
        boolean pathClear = true; // Ensure the path is valid
        int jumpedEnemyColumn = -1;
        int jumpedEnemyRow = -1;
        for (int i = 1; i <= columnStep; i++) {
            int intermediateColumn = king.lastColumn + i * colDirection;
            int intermediateRow = king.lastRow + i * rowDirection;
            Square intermediateSquare = squares[intermediateColumn][intermediateRow];
            Soldier middleSoldier = intermediateSquare.soldier;
            if (middleSoldier != null) {
                if (middleSoldier.side != king.side && !hasJumped) {
                    // Valid jump over an opponent soldier
                    hasJumped = true;
                    jumpedEnemyColumn = intermediateColumn;
                    jumpedEnemyRow = intermediateRow;
                    Log.d("KingMove", "Jumping over enemy soldier at column=" + intermediateColumn + ", row=" + intermediateRow);
                } else {
                    // Either jumping over multiple pieces or over a teammate
                    Log.d("KingMove", "Move failed: path is blocked or invalid jump.");
                    pathClear = false;
                    break;
                }
            }
        }
        // Ensure the destination square is empty
        Square destinationSquare = squares[king.column][king.row];
        if (!pathClear || destinationSquare.soldier != null) {
            Log.d("KingMove", "Move failed: destination is not valid.");
            return false;
        }

        // Validate landing for jumps
        if (hasJumped) {
            int jumpDistance = Math.abs(king.column - jumpedEnemyColumn);
            if (jumpDistance < 1) {
                Log.d("KingMove", "Move failed: jump landing is too close.");
                return false;
            }
            // Remove the jumped-over soldier
            squares[jumpedEnemyColumn][jumpedEnemyRow].soldier = null;
            isSoldierJumped = true;//King made a jump
            Log.d("KingMove", "Removed jumped-over enemy soldier at column=" + jumpedEnemyColumn + ", row=" + jumpedEnemyRow);
        }
        Log.d("KingMove", "Move is valid.");
        return true;
    }

    public boolean isValidMove(Soldier soldier) {
        int step = Math.abs(soldier.column - soldier.lastColumn);

        if (step == 1) {
            if (soldier.side == 1) {
                if (isValidSingleStepForSide1(soldier)) {
                    return true;
                }
            } else if (isValidSingleStepForSide2(soldier)) {
                return true;
            }
        } else if (step == 2) {
            if (soldier.side == 1) {
                if (isValidJumpForSide1(soldier)) {
                    isSoldierJumped = true;
                    displyWinner();
                    return true;
                }
            } else if (isValidJumpForSide2(soldier)) {
                isSoldierJumped = true;
                displyWinner();
                return true;
            }
        }
        return false;
    }

    private boolean isValidSingleStepForSide1(Soldier soldier) {
        if (soldier.row == 0) {
            return soldier.column - 1 == soldier.lastColumn && soldier.row + 1 == soldier.lastRow;
        } else if (soldier.row == 7) {
            return soldier.column - 1 == soldier.lastColumn && soldier.row - 1 == soldier.lastRow;
        } else {
            return (soldier.column - 1 == soldier.lastColumn && soldier.row + 1 == soldier.lastRow) ||
                    (soldier.column - 1 == soldier.lastColumn && soldier.row - 1 == soldier.lastRow);
        }
    }

    private boolean isValidSingleStepForSide2(Soldier soldier) {
        if (soldier.row == 0) {
            return soldier.column + 1 == soldier.lastColumn && soldier.row + 1 == soldier.lastRow;
        } else if (soldier.row == 7) {
            return soldier.column + 1 == soldier.lastColumn && soldier.row - 1 == soldier.lastRow;
        } else {
            return (soldier.column + 1 == soldier.lastColumn && soldier.row + 1 == soldier.lastRow) ||
                    (soldier.column + 1 == soldier.lastColumn && soldier.row - 1 == soldier.lastRow);
        }
    }

    private boolean isValidJumpForSide1(Soldier soldier) {
        if (soldier.column - 2 == soldier.lastColumn && soldier.row + 2 == soldier.lastRow &&
                isEnemySoldier(squares[soldier.column - 1][soldier.row + 1].soldier, soldier.side)) {
            squares[soldier.column - 1][soldier.row + 1].soldier = null;
            return true;
        } else if (soldier.column - 2 == soldier.lastColumn && soldier.row - 2 == soldier.lastRow &&
                isEnemySoldier(squares[soldier.column - 1][soldier.row - 1].soldier, soldier.side)) {
            squares[soldier.column - 1][soldier.row - 1].soldier = null;
            return true;
        }
        return false;
    }

    private boolean isValidJumpForSide2(Soldier soldier) {
        if (soldier.column + 2 == soldier.lastColumn && soldier.row + 2 == soldier.lastRow &&
                isEnemySoldier(squares[soldier.column + 1][soldier.row + 1].soldier, soldier.side)) {
            squares[soldier.column + 1][soldier.row + 1].soldier = null;
            return true;
        } else if (soldier.column + 2 == soldier.lastColumn && soldier.row - 2 == soldier.lastRow &&
                isEnemySoldier(squares[soldier.column + 1][soldier.row - 1].soldier, soldier.side)) {
            squares[soldier.column + 1][soldier.row - 1].soldier = null;
            return true;
        }
        return false;
    }


    // Updates soldier's column and row
    private void updateColumnAndRow(Soldier soldier) {
        for (int i = 0; i < NUM_OF_SQUARES; i++) {
            for (int j = 0; j < NUM_OF_SQUARES; j++) {
                if (squares[i][j].didUserTouchMe(soldier.x, soldier.y)) {
                    soldier.column = i;
                    soldier.row = j;
                    break;
                }
            }
        }
    }

    // Helper method to check if a soldier belongs to the enemy side
    private boolean isEnemySoldier(Soldier soldier, int currentSide) {
        return soldier != null && soldier.side != currentSide;
    }

    private void updateLastPosition(Soldier soldier) {
        // Update only when soldier has successfully snapped into this square
        soldier.lastX = soldier.x;
        soldier.lastY = soldier.y;

        // Update soldier's current row and column
        soldier.lastColumn = soldier.column;
        soldier.lastRow = soldier.row;
    }

    private int isGameOver() {
        boolean side1HasSoldiers = false;
        boolean side2HasSoldiers = false;
        for (int i = 0; i < NUM_OF_SQUARES; i++) {
            for (int j = 0; j < NUM_OF_SQUARES; j++) {
                Soldier soldier = squares[i][j].soldier;
                if (soldier != null) {
                    if (soldier.side == 1) {
                        side1HasSoldiers = true;
                    } else if (soldier.side == 2) {
                        side2HasSoldiers = true;
                    }
                }
                if (side1HasSoldiers && side2HasSoldiers) {
                    return 0; // Game is not over, both sides have soldiers
                }
            }
        }
        if (!side1HasSoldiers) {
            return 2; // Side 2 wins
        } else if (!side2HasSoldiers) {
            return 1; // Side 1 wins
        }
        return 0; // This shouldn't happen, but just in case
    }

    public void displyWinner() {
        if (isSoldierJumped == true) {
            int winnerside = isGameOver();
            if (winnerside == 1)
                Toast.makeText(getContext(), "The winner side is Red!!!", Toast.LENGTH_SHORT).show();
            if (winnerside == 2)
                Toast.makeText(getContext(), "The winner side is Blue!!!", Toast.LENGTH_SHORT).show();
        }
    }
}




