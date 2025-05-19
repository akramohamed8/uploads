package controller;

import engine.Game;
import engine.board.Board;
import engine.board.Cell;
import engine.board.SafeZone;
import exception.GameException;
import exception.InvalidCardException;
import exception.InvalidMarbleException;
import exception.SplitOutOfRangeException;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import model.Colour;
import model.card.Card;
import model.player.Marble;
import model.player.Player;
import view.GameView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

public class GameController {
    private final Game game;
    private final GameView view;

    public GameController(Game game, GameView view) {
        this.game = game;
        this.view = view;
        initialize();
    }

    private void initialize() {
        // Initial board and player info
        updateView();

        // Button actions
        view.getPlayButton().setOnAction(e -> handlePlayAction());
        view.getCancelButton().setOnAction(e -> handleCancelAction());
        // Card selection
        view.cardArea.setOnMouseClicked(event -> handleCardSelection(event));
        // Marble selection will be handled after board is created
    }

    private void handlePlayAction() {
        if (!isHumanTurn()) return;
        try {
            int splitDistance = view.getSplitDistance();
            game.playPlayerTurn();
            view.showNotification("Turn played successfully.");
            game.endPlayerTurn();
            view.clearSelections();
            updateView();
            if (checkWinner()) return;
            // After human turn, start CPU turns
            handleCPUTurns();
        } catch (GameException ex) {
            view.showNotification("Exception: " + ex.getMessage());
        }
    }

    private void handleCancelAction() {
        game.deselectAll();
        view.clearSelections();
        view.showNotification("Selection cancelled.");
    }

    private void handleCardSelection(MouseEvent event) {
        if (!isHumanTurn()) return;
        Object source = event.getTarget();
        for (view.GameView.CardView cardView : view.cardViews) {
            if (cardView == source || cardView.getChildren().contains(source)) {
                try {
                    game.selectCard(cardView.getCard());
                    view.selectCard(cardView.getCard());
                    view.showNotification("Selected card: " + cardView.getCard().getName());
                } catch (InvalidCardException ex) {
                    view.showNotification("Exception: " + ex.getMessage());
                }
                break;
            }
        }
    }

    private void enableMarbleSelection() {
        for (Map.Entry<engine.board.Cell, javafx.scene.layout.StackPane> entry : view.cellViews.entrySet()) {
            StackPane cellView = entry.getValue();
            engine.board.Cell cell = entry.getKey();
            cellView.setOnMouseClicked(e -> {
                if (!isHumanTurn()) return;
                if (cell.getMarble() != null) {
                    try {
                        game.selectMarble(cell.getMarble());
                        view.selectMarble(cell.getMarble(), true);
                        view.showNotification("Selected marble.");
                    } catch (InvalidMarbleException ex) {
                        view.showNotification("Exception: " + ex.getMessage());
                    }
                }
            });
        }
    }

    private boolean isHumanTurn() {
        return isHumanTurn(getCurrentPlayerIndex());
    }

    private boolean isHumanTurn(int playerIndex) {
        return game.getPlayers().get(playerIndex).getName().equals(game.getPlayers().get(0).getName());
    }

    private void updateView() {
        Board board = game.getBoard();
        ArrayList<Cell> track = board.getTrack();
        ArrayList<ArrayList<Cell>> safeZones = new ArrayList<>();
        for (SafeZone sz : board.getSafeZones()) {
            safeZones.add(sz.getCells());
        }
        view.createBoard(track, safeZones);
        view.updateBoard(track, safeZones);
        int currentPlayerIndex = getCurrentPlayerIndex();
        int nextPlayerIndex = (currentPlayerIndex + 1) % game.getPlayers().size();
        view.updatePlayerInfo(game.getPlayers().get(currentPlayerIndex), getTurn());
        view.updateHomeZones(game.getPlayers());
        view.updatePlayerCards(game.getPlayers().get(currentPlayerIndex).getHand(), game.getActivePlayerColour());
        // Update next player
        Player nextPlayer = game.getPlayers().get(nextPlayerIndex);
        view.updateNextPlayer(nextPlayer.getName(), nextPlayer.getColour().toString());
        // Update fire pit
        String firePitCard = "None";
        if (!game.getFirePit().isEmpty()) {
            firePitCard = game.getFirePit().get(game.getFirePit().size() - 1).getName();
        }
        view.updateFirePit(firePitCard);
        // Enable marble selection for the new board
        Platform.runLater(this::enableMarbleSelection);
    }

    private int getCurrentPlayerIndex() {
        // Assuming Game exposes currentPlayerIndex (if not, add a getter)
        try {
            java.lang.reflect.Field f = game.getClass().getDeclaredField("currentPlayerIndex");
            f.setAccessible(true);
            return f.getInt(game);
        } catch (Exception e) {
            return 0;
        }
    }

    private int getTurn() {
        // Assuming Game exposes turn (if not, add a getter)
        try {
            java.lang.reflect.Field f = game.getClass().getDeclaredField("turn");
            f.setAccessible(true);
            return f.getInt(game);
        } catch (Exception e) {
            return 0;
        }
    }

    private Card getSelectedCard() {
        for (view.GameView.CardView cardView : view.cardViews) {
            if (cardView.background.getStroke() == javafx.scene.paint.Color.YELLOW) {
                return cardView.getCard();
            }
        }
        return null;
    }

    private void handleCPUTurns() {
        int currentPlayerIndex = getCurrentPlayerIndex();
        int nextPlayerIndex = (currentPlayerIndex + 1) % game.getPlayers().size();
        // If next player is CPU, automate their turn
        if (!isHumanTurn(nextPlayerIndex)) {
            PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
            pause.setOnFinished(e -> {
                try {
                    game.playPlayerTurn();
                    view.showNotification(game.getPlayers().get(nextPlayerIndex).getName() + " played their turn.");
                    game.endPlayerTurn();
                    updateView();
                    if (checkWinner()) return;
                    handleCPUTurns(); // Continue to next CPU if needed
                } catch (GameException ex) {
                    view.showNotification("Exception: " + ex.getMessage());
                    game.endPlayerTurn();
                    updateView();
                    if (checkWinner()) return;
                    handleCPUTurns();
                } catch (Exception ex) {
                    view.showNotification("Error: " + ex.getMessage());
                    game.endPlayerTurn();
                    updateView();
                    if (checkWinner()) return;
                    handleCPUTurns();
                }
            });
            pause.play();
        }
    }

    private boolean checkWinner() {
        model.Colour winner = game.checkWin();
        if (winner != null) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Game Over");
            alert.setHeaderText("We have a winner!");
            alert.setContentText("Winner: " + winner.toString());
            alert.showAndWait();
            return true;
        }
        return false;
    }

    public void fieldMarbleShortcut() {
        if (!isHumanTurn()) return;
        try {
            game.fieldMarble();
            view.showNotification("Marble fielded using shortcut.");
            updateView();
        } catch (Exception ex) {
            view.showNotification("Exception: " + ex.getMessage());
        }
    }
} 