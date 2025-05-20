package controller;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import model.Colour;
import model.card.Card;
import model.player.Marble;
import model.player.Player;
import model.player.CPU;
import engine.Game;
import engine.board.Board;
import engine.board.Cell;
import engine.board.CellType;
import view.GameView;
import exception.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

public class GameController {
    private final Game game;
    private final GameView view;
    private Card selectedCard;
    private Marble selectedMarble;
    private Timer computerTurnTimer;
    private boolean isComputerTurn;
    private int currentPlayerIndex;

    public GameController(Game game, GameView view) {
        this.game = game;
        this.view = view;
        this.currentPlayerIndex = 0;
        setupEventHandlers();
        // Connect cell click events
        view.setCellClickListener((zone, position) -> handleCellClick(zone, position));
        // Connect card click events
        view.setCardClickListener(card -> handleCardSelection(card));
        updateGameView();
    }

    private void setupEventHandlers() {
        view.getPlayButton().setOnAction(e -> handlePlayButton());
        view.getCancelButton().setOnAction(e -> handleCancelButton());
    }

    private void handlePlayButton() {
        try {
            if (selectedCard == null) {
                view.updateStatus("No card selected");
                throw new InvalidCardException("No card selected");
            }

            game.selectCard(selectedCard);
            
            if (selectedMarble != null) {
                game.selectMarble(selectedMarble);
            }

            if (selectedCard.toString().startsWith("7")) {
                String splitDistance = view.getSplitDistance();
                if (splitDistance == null || splitDistance.trim().isEmpty()) {
                    view.updateStatus("Please enter a split distance");
                    throw new SplitOutOfRangeException("Please enter a split distance");
                }
                try {
                    int distance = Integer.parseInt(splitDistance);
                    game.editSplitDistance(distance);
                } catch (NumberFormatException e) {
                    view.updateStatus("Invalid split distance");
                    throw new SplitOutOfRangeException("Invalid split distance");
                }
            }

            if (game.canPlayTurn()) {
                game.playPlayerTurn();
                game.endPlayerTurn();
                currentPlayerIndex = (currentPlayerIndex + 1) % game.getPlayers().size();
                clearSelections();
                updateGameView();
                checkWinCondition();
                startComputerTurn();
            } else {
                view.updateStatus("Cannot play turn with current selection");
                throw new InvalidCardException("Cannot play turn with current selection");
            }
        } catch (GameException e) {
            showError(e.getMessage());
        }
    }

    private void handleCancelButton() {
        game.deselectAll();
        clearSelections();
        updateGameView();
    }

    private void handleCardSelection(Card card) {
        if (card == null) return;
        try {
            // If the same card is clicked again, deselect it
            if (selectedCard == card) {
                selectedCard = null;
                view.highlightCard(card, false);
                game.deselectAll();
                view.highlightValidMoves(new ArrayList<>());
                return;
            }
            // Deselect previously selected card if any
            if (selectedCard != null) {
                view.highlightCard(selectedCard, false);
            }
            // Select the new card
            selectedCard = card;
            view.highlightCard(card, true);
            game.selectCard(card);
            // Only allow actionable marbles for this card
            List<String> validMoves = new ArrayList<>();
            Player currentPlayer = game.getPlayers().get(currentPlayerIndex);
            List<Marble> actionable = game.getBoard().getActionableMarbles();
            if (card.getName().equalsIgnoreCase("Ace") || card.getName().equalsIgnoreCase("King")) {
                // Allow fielding from home if possible
                for (int i = 0; i < currentPlayer.getMarbles().size(); i++) {
                    Marble marble = currentPlayer.getMarbles().get(i);
                    if (!actionable.contains(marble)) {
                        validMoves.add("home_" + i);
                    }
                }
            }
            // Highlight only actionable marbles on track/safe
            for (Marble marble : actionable) {
                // Track
                int idx = game.getBoard().getTrack().indexOf(marble);
                if (idx != -1) validMoves.add("track_" + idx);
                // Safe zone
                for (int si = 0; si < 16; si++) {
                    final int s = si;
                    if (game.getBoard().getSafeZones().stream().anyMatch(sz -> sz.getCells().size() > s && sz.getCells().get(s).getMarble() == marble)) {
                        validMoves.add("safe_" + s);
                    }
                }
            }
            view.highlightValidMoves(validMoves);
            updateGameView();
        } catch (Exception e) {
            showError("Error selecting card: " + e.getMessage());
        }
    }

    public void handleCellClick(String zone, int position) {
        try {
            if (selectedCard == null) {
                view.updateStatus("Please select a card first");
                return;
            }

            // Find the cell in the appropriate zone
            Cell cell = null;
            if (zone.equals("track")) {
                ArrayList<Cell> track = game.getBoard().getTrack();
                cell = track.get(position);
            } else if (zone.equals("home")) {
                Player currentPlayer = game.getPlayers().get(currentPlayerIndex);
                if (position < currentPlayer.getMarbles().size()) {
                    Marble marble = currentPlayer.getMarbles().get(position);
                    if (marble != null) {
                        selectedMarble = marble;
                        view.highlightCell(zone, position, true);
                        view.updateStatus("Selected marble in home zone");
                        return;
                    }
                }
            } else if (zone.equals("safe")) {
                ArrayList<Cell> safeZone = game.getBoard().getSafeZones().stream()
                    .filter(sz -> sz.getColour() == game.getPlayers().get(currentPlayerIndex).getColour())
                    .findFirst()
                    .map(sz -> sz.getCells())
                    .orElse(new ArrayList<>());
                if (position < safeZone.size()) {
                    cell = safeZone.get(position);
                }
            }

            if (cell == null) {
                view.updateStatus("Invalid cell selected");
                return;
            }

            Marble marble = cell.getMarble();
            if (marble == null) {
                view.updateStatus("No marble in selected cell");
                return;
            }

            if (selectedMarble == marble) {
                selectedMarble = null;
                view.highlightCell(zone, position, false);
                view.updateStatus("Deselected marble");
            } else {
                if (selectedMarble != null) {
                    // Find and unhighlight previous selection
                    String prevZone = findMarbleZone(selectedMarble);
                    int prevPos = findMarblePosition(selectedMarble, prevZone);
                    if (prevZone != null) {
                        view.highlightCell(prevZone, prevPos, false);
                    }
                }
                selectedMarble = marble;
                view.highlightCell(zone, position, true);
                view.updateStatus("Selected marble in " + zone + " zone");
            }
            updateGameView();
        } catch (Exception e) {
            showError("Error handling cell click: " + e.getMessage());
        }
    }

    private String findMarbleZone(Marble marble) {
        // Check track
        ArrayList<Cell> track = game.getBoard().getTrack();
        for (int i = 0; i < track.size(); i++) {
            if (track.get(i).getMarble() == marble) {
                return "track";
            }
        }

        // Check safe zones
        for (Colour colour : Colour.values()) {
            ArrayList<Cell> safeZone = game.getBoard().getSafeZones().stream()
                .filter(sz -> sz.getColour() == colour)
                .findFirst()
                .map(sz -> sz.getCells())
                .orElse(new ArrayList<>());
            for (int i = 0; i < safeZone.size(); i++) {
                if (safeZone.get(i).getMarble() == marble) {
                    return "safe";
                }
            }
        }

        // Check home zone
        for (Player player : game.getPlayers()) {
            if (player.getMarbles().contains(marble)) {
                return "home";
            }
        }

        return null;
    }

    private int findMarblePosition(Marble marble, String zone) {
        if (zone == null) return -1;

        if (zone.equals("track")) {
            ArrayList<Cell> track = game.getBoard().getTrack();
            for (int i = 0; i < track.size(); i++) {
                if (track.get(i).getMarble() == marble) {
                    return i;
                }
            }
        } else if (zone.equals("safe")) {
            ArrayList<Cell> safeZone = game.getBoard().getSafeZones().stream()
                .filter(sz -> sz.getColour() == marble.getColour())
                .findFirst()
                .map(sz -> sz.getCells())
                .orElse(new ArrayList<>());
            for (int i = 0; i < safeZone.size(); i++) {
                if (safeZone.get(i).getMarble() == marble) {
                    return i;
                }
            }
        } else if (zone.equals("home")) {
            for (Player player : game.getPlayers()) {
                if (player.getMarbles().contains(marble)) {
                    return player.getMarbles().indexOf(marble);
                }
            }
        }

        return -1;
    }

    private void startComputerTurn() {
        if (game.getPlayers().get(currentPlayerIndex) instanceof CPU) {
            isComputerTurn = true;
            view.updateStatus("Computer player's turn...");
            view.getPlayButton().setDisable(true);
            view.getCancelButton().setDisable(true);
            view.showComputerTurnProgress(true);

            computerTurnTimer = new Timer();
            final long startTime = System.currentTimeMillis();
            final long duration = 2000; // 2 seconds

            computerTurnTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    long elapsed = System.currentTimeMillis() - startTime;
                    double progress = Math.min(1.0, (double) elapsed / duration);
                    Platform.runLater(() -> view.updateComputerTurnProgress(progress));

                    if (elapsed >= duration) {
                        Platform.runLater(() -> {
                            try {
                                game.playPlayerTurn();
                                game.endPlayerTurn();
                                currentPlayerIndex = (currentPlayerIndex + 1) % game.getPlayers().size();
                                updateGameView();
                                checkWinCondition();
                                isComputerTurn = false;
                                view.getPlayButton().setDisable(false);
                                view.getCancelButton().setDisable(false);
                                view.showComputerTurnProgress(false);
                            } catch (GameException e) {
                                showError(e.getMessage());
                            }
                        });
                        cancel();
                    }
                }
            }, 0, 50); // Update every 50ms
        }
    }

    private void updateGameView() {
        Player currentPlayer = game.getPlayers().get(currentPlayerIndex);
        view.updatePlayerInfo(currentPlayer.getName(), currentPlayer.getColour(), 1);
        view.updateUserAreaName(currentPlayer.getName());
        int nextIndex = (currentPlayerIndex + 1) % game.getPlayers().size();
        Player nextPlayer = game.getPlayers().get(nextIndex);
        view.updateNextPlayer(nextPlayer.getName(), nextPlayer.getColour());
        view.updateCards(currentPlayer.getHand());
        updateBoard();
        view.showSplitDistanceInput(selectedCard != null && selectedCard.toString().startsWith("7"));
        view.getPlayButton().setDisable(false);
    }

    private void updateBoard() {
        // Clear all cells first
        for (int i = 0; i < 100; i++) {
            view.updateMarble("track", i, null);
        }
        for (int i = 0; i < 4; i++) {
            view.updateMarble("home", i, null);
            view.updateMarble("safe", i, null);
        }

        // Update marbles on track
        ArrayList<Cell> track = game.getBoard().getTrack();
        for (int i = 0; i < track.size(); i++) {
            Cell cell = track.get(i);
            if (cell != null && cell.getMarble() != null) {
                view.updateMarble("track", i, cell.getMarble().getColour());
            }
        }

        // Update marbles in home zones
        for (Player player : game.getPlayers()) {
            for (int i = 0; i < player.getMarbles().size(); i++) {
                Marble marble = player.getMarbles().get(i);
                if (marble != null) {
                    view.updateMarble("home", i, marble.getColour());
                }
            }
        }

        // Update marbles in safe zones
        for (Colour colour : Colour.values()) {
            ArrayList<Cell> safeZone = game.getBoard().getSafeZones().stream()
                .filter(sz -> sz.getColour() == colour)
                .findFirst()
                .map(sz -> sz.getCells())
                .orElse(new ArrayList<>());
            
            for (int i = 0; i < safeZone.size(); i++) {
                Cell cell = safeZone.get(i);
                if (cell != null && cell.getMarble() != null) {
                    view.updateMarble("safe", i, cell.getMarble().getColour());
                }
            }
        }
    }

    private void clearSelections() {
        if (selectedCard != null) {
            view.highlightCard(selectedCard, false);
            selectedCard = null;
        }
        if (selectedMarble != null) {
            String zone = findMarbleZone(selectedMarble);
            int position = findMarblePosition(selectedMarble, zone);
            if (zone != null && position != -1) {
                view.highlightCell(zone, position, false);
            }
            selectedMarble = null;
        }
        view.clearSplitDistance();
    }

    private void checkWinCondition() {
        Colour winner = game.checkWin();
        if (winner != null) {
            showGameOver(winner);
        }
    }

    private void showGameOver(Colour winner) {
        Platform.runLater(() -> {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Game Over");

            VBox content = new VBox(10);
            content.setAlignment(Pos.CENTER);
            content.setPadding(new Insets(20));
            content.getChildren().add(new Label(winner + " wins!"));

            Button okButton = new Button("OK");
            okButton.setOnAction(e -> dialog.close());
            content.getChildren().add(okButton);

            Scene scene = new Scene(content);
            dialog.setScene(scene);
            dialog.showAndWait();
        });
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Error");

            VBox content = new VBox(10);
            content.setAlignment(Pos.CENTER);
            content.setPadding(new Insets(20));
            content.getChildren().add(new Label(message));

            Button okButton = new Button("OK");
            okButton.setOnAction(e -> dialog.close());
            content.getChildren().add(okButton);

            Scene scene = new Scene(content);
            dialog.setScene(scene);
            dialog.showAndWait();
        });
    }

    public void fieldMarbleShortcut() {
        if (!isComputerTurn) {
            try {
                game.fieldMarble();
                updateGameView();
            } catch (GameException e) {
                showError(e.getMessage());
            }
        }
    }

    public void shutdown() {
        if (computerTurnTimer != null) {
            computerTurnTimer.cancel();
        }
    }
}
