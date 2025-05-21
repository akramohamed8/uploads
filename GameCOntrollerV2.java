package controller;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.animation.TranslateTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
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
import java.util.Random;

public class GameController {
    private final Game game;
    private final GameView view;
    private Card selectedCard;
    private Marble selectedMarble;
    private Marble secondMarble; // For Seven card
    private Timer computerTurnTimer;
    private boolean isComputerTurn;
    private int currentPlayerIndex;
    private final Random random = new Random();

    public GameController(Game game, GameView view) {
        this.game = game;
        this.view = view;
        this.currentPlayerIndex = 0;
        setupEventHandlers();
        // Connect cell click events
        view.setCellClickListener((zone, position) -> handleCellClick(zone, position));
        // Connect card click events
        view.setCardClickListener(card -> handleCardSelection(card));
    }

    public void initializeGame() {
        // Deal initial cards to all players
        game.dealCards();
        updateGameView();
        
        // Display initial game message
        view.updateStatus("Game started! Your turn");
        
        // Highlight valid moves for player
        showValidActionsForCurrentPlayer();
    }

    private void setupEventHandlers() {
        view.getPlayButton().setOnAction(e -> handlePlayButton());
        view.getCancelButton().setOnAction(e -> handleCancelButton());
        
        // Add keyboard event handler for 'F' key to field marbles
        view.getRoot().setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("F")) {
                handleFieldMarble();
            }
        });
    }
    
    private void handleFieldMarble() {
        try {
            if (selectedCard == null) {
                view.updateStatus("Select an Ace or King card first");
                return;
            }
            
            // Check if selected card is Ace or King
            if (!selectedCard.getName().equalsIgnoreCase("Ace") && 
                !selectedCard.getName().equalsIgnoreCase("King")) {
                view.updateStatus("Need Ace or King to field a marble");
                return;
            }
            
            Player currentPlayer = game.getPlayers().get(currentPlayerIndex);
            List<Marble> homeMarbles = currentPlayer.getMarbles();
            
            if (homeMarbles.isEmpty()) {
                view.updateStatus("No marbles in home to field");
                return;
            }
            
            // Try to field the first available marble
            for (Marble marble : homeMarbles) {
                if (game.canFieldMarble(marble)) {
                    selectedMarble = marble;
                    view.updateStatus("Attempting to field marble");
                    handlePlayButton();
                    return;
                }
            }
            
            view.updateStatus("Cannot field marble - base cell occupied");
        } catch (Exception e) {
            showError("Error fielding marble: " + e.getMessage());
        }
    }

    private void showValidActionsForCurrentPlayer() {
        if (selectedCard != null) {
            List<String> validMoves = getValidMovesForSelectedCard();
            view.highlightValidMoves(validMoves);
        } else {
            view.highlightValidMoves(new ArrayList<>());
        }
    }

    private List<String> getValidMovesForSelectedCard() {
        List<String> validMoves = new ArrayList<>();
        
        if (selectedCard == null) return validMoves;
        
        Player currentPlayer = game.getPlayers().get(currentPlayerIndex);
        List<Marble> actionable = game.getBoard().getActionableMarbles();
        
        // For Ace or King, show home marbles
        if (selectedCard.getName().equalsIgnoreCase("Ace") || selectedCard.getName().equalsIgnoreCase("King")) {
            // Allow fielding from home if possible
            for (int i = 0; i < currentPlayer.getMarbles().size(); i++) {
                Marble marble = currentPlayer.getMarbles().get(i);
                if (marble != null && game.canFieldMarble(marble)) {
                    validMoves.add("home_" + i);
                }
            }
        }
        
        // Show track marbles
        for (Marble marble : actionable) {
            if (marble.getColour() == currentPlayer.getColour()) {
                // Track positions
                int idx = game.getBoard().getTrack().indexOf(marble);
                if (idx != -1) validMoves.add("track_" + idx);
                
                // Safe zone positions
                for (int si = 0; si < 16; si++) {
                    final int s = si;
                    if (game.getBoard().getSafeZones().stream()
                        .filter(sz -> sz.getColour() == currentPlayer.getColour())
                        .anyMatch(sz -> sz.getCells().size() > s && sz.getCells().get(s).getMarble() == marble)) {
                        validMoves.add("safe_" + s);
                    }
                }
            }
        }
        
        // For special cards like Jack or Burner, also show opponent marbles
        if (selectedCard.getName().equalsIgnoreCase("Jack") || selectedCard.getName().contains("Burner")) {
            for (Marble marble : actionable) {
                if (marble.getColour() != currentPlayer.getColour()) {
                    int idx = game.getBoard().getTrack().indexOf(marble);
                    if (idx != -1) validMoves.add("track_" + idx);
                }
            }
        }
        
        return validMoves;
    }

    public void handlePlayButton() {
        try {
            if (selectedCard == null) {
                view.updateStatus("No card selected");
                throw new InvalidCardException("No card selected");
            }

            game.selectCard(selectedCard);
            
            if (selectedMarble != null) {
                game.selectMarble(selectedMarble);
            }
            
            if (secondMarble != null && selectedCard.toString().startsWith("7")) {
                game.selectSecondMarble(secondMarble);
            }

            // Handle split distance for Seven card
            if (selectedCard.toString().startsWith("7")) {
                String splitDistance = view.getSplitDistance();
                if (splitDistance == null || splitDistance.trim().isEmpty()) {
                    view.updateStatus("Please enter a split distance");
                    throw new SplitOutOfRangeException("Please enter a split distance");
                }
                try {
                    int distance = Integer.parseInt(splitDistance);
                    if (distance < 1 || distance > 6) {
                        throw new SplitOutOfRangeException("Split distance must be between 1 and 6");
                    }
                    game.editSplitDistance(distance);
                } catch (NumberFormatException e) {
                    view.updateStatus("Invalid split distance");
                    throw new SplitOutOfRangeException("Invalid split distance");
                }
            }

            if (game.canPlayTurn()) {
                // Animate the card being played
                animateCardPlay(selectedCard);
                
                // Execute the turn
                game.playPlayerTurn();
                
                // End the turn and update game state
                game.endPlayerTurn();
                currentPlayerIndex = (currentPlayerIndex + 1) % game.getPlayers().size();
                clearSelections();
                updateGameView();
                checkWinCondition();
                
                // Start computer turn if next player is CPU
                startComputerTurn();
            } else {
                view.updateStatus("Cannot play turn with current selection");
                throw new InvalidCardException("Cannot play turn with current selection");
            }
        } catch (GameException e) {
            showError(e.getMessage());
        }
    }

    public void handleCancelButton() {
        game.deselectAll();
        clearSelections();
        updateGameView();
        view.updateStatus("Selection cleared");
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
                view.updateCardDescription(null);
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
            
            // Show card description
            view.updateCardDescription(card);
            
            // Reset marble selections when changing cards
            if (selectedMarble != null) {
                String zone = findMarbleZone(selectedMarble);
                int position = findMarblePosition(selectedMarble, zone);
                if (zone != null && position != -1) {
                    view.highlightCell(zone, position, false);
                }
                selectedMarble = null;
            }
            
            if (secondMarble != null) {
                String zone = findMarbleZone(secondMarble);
                int position = findMarblePosition(secondMarble, zone);
                if (zone != null && position != -1) {
                    view.highlightCell(zone, position, false);
                }
                secondMarble = null;
            }
            
            // Show split distance input if Seven card
            view.showSplitDistanceInput(card.toString().startsWith("7"));
            
            // Show valid moves for this card
            showValidActionsForCurrentPlayer();
            
            view.updateStatus("Selected card: " + card.toString());
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
            Marble marble = null;
            
            if (zone.equals("track")) {
                ArrayList<Cell> track = game.getBoard().getTrack();
                if (position < track.size()) {
                    cell = track.get(position);
                    marble = cell.getMarble();
                }
            } else if (zone.equals("home")) {
                Player currentPlayer = game.getPlayers().get(currentPlayerIndex);
                if (position < currentPlayer.getMarbles().size()) {
                    marble = currentPlayer.getMarbles().get(position);
                }
            } else if (zone.equals("safe")) {
                ArrayList<Cell> safeZone = game.getBoard().getSafeZones().stream()
                    .filter(sz -> sz.getColour() == game.getPlayers().get(currentPlayerIndex).getColour())
                    .findFirst()
                    .map(sz -> sz.getCells())
                    .orElse(new ArrayList<>());
                if (position < safeZone.size()) {
                    cell = safeZone.get(position);
                    if (cell != null) {
                        marble = cell.getMarble();
                    }
                }
            }

            // Handle the Seven card case (may need two marbles)
            if (selectedCard.toString().startsWith("7")) {
                handleSevenCardCellClick(zone, position, marble);
                return;
            }
            
            // Handle Jack card case (swapping)
            if (selectedCard.toString().startsWith("J")) {
                handleJackCardCellClick(zone, position, marble);
                return;
            }
            
            // Standard case - single marble selection
            if (marble == null) {
                view.updateStatus("No marble in selected cell");
                return;
            }

            // If clicking the same marble, deselect it
            if (selectedMarble == marble) {
                selectedMarble = null;
                view.highlightCell(zone, position, false);
                view.updateStatus("Deselected marble");
            } else {
                // Deselect previous marble if any
                if (selectedMarble != null) {
                    String prevZone = findMarbleZone(selectedMarble);
                    int prevPos = findMarblePosition(selectedMarble, prevZone);
                    if (prevZone != null && prevPos != -1) {
                        view.highlightCell(prevZone, prevPos, false);
                    }
                }
                
                // For opponent's marble, check if the card allows selecting it
                if (marble.getColour() != game.getPlayers().get(currentPlayerIndex).getColour()) {
                    if (!selectedCard.toString().startsWith("J") && !selectedCard.getName().contains("Burner")) {
                        view.updateStatus("Cannot select opponent's marble with this card");
                        return;
                    }
                }
                
                selectedMarble = marble;
                view.highlightCell(zone, position, true);
                view.updateStatus("Selected marble in " + zone + " zone");
            }
        } catch (Exception e) {
            showError("Error handling cell click: " + e.getMessage());
        }
    }
    
    private void handleSevenCardCellClick(String zone, int position, Marble marble) {
        if (marble == null) {
            view.updateStatus("No marble in selected cell");
            return;
        }
        
        // For Seven card, we need to select two marbles
        if (selectedMarble == null) {
            // Select first marble
            selectedMarble = marble;
            view.highlightCell(zone, position, true);
            view.updateStatus("Selected first marble for Seven card. Now select second marble or same marble again.");
        } else if (secondMarble == null && selectedMarble != marble) {
            // Select second marble if it's different from the first
            secondMarble = marble;
            view.highlightCell(zone, position, true);
            view.updateStatus("Selected second marble for Seven card.");
        } else if (selectedMarble == marble) {
            // Deselect if clicking same marble again
            selectedMarble = null;
            view.highlightCell(zone, position, false);
            view.updateStatus("Deselected first marble for Seven card.");
        } else if (secondMarble == marble) {
            // Deselect second marble if clicking it again
            secondMarble = null;
            view.highlightCell(zone, position, false);
            view.updateStatus("Deselected second marble for Seven card.");
        }
    }
    
    private void handleJackCardCellClick(String zone, int position, Marble marble) {
        if (marble == null) {
            view.updateStatus("No marble in selected cell");
            return;
        }
        
        // For Jack card (swapping), need to select one of our marbles and one opponent marble
        if (selectedMarble == null) {
            // Select first marble
            selectedMarble = marble;
            view.highlightCell(zone, position, true);
            view.updateStatus("Selected first marble for Jack card. Now select an opponent's marble to swap with.");
        } else if (secondMarble == null && selectedMarble != marble) {
            // For Jack, one marble should be player's and one should be opponent's
            if ((selectedMarble.getColour() == game.getPlayers().get(currentPlayerIndex).getColour() && 
                 marble.getColour() != game.getPlayers().get(currentPlayerIndex).getColour()) ||
                (selectedMarble.getColour() != game.getPlayers().get(currentPlayerIndex).getColour() && 
                 marble.getColour() == game.getPlayers().get(currentPlayerIndex).getColour())) {
                
                secondMarble = marble;
                view.highlightCell(zone, position, true);
                view.updateStatus("Selected second marble for Jack card.");
            } else {
                view.updateStatus("For Jack card, select one of your marbles and one opponent marble.");
            }
        } else if (selectedMarble == marble) {
            // Deselect if clicking same marble again
            selectedMarble = null;
            view.highlightCell(zone, position, false);
            view.updateStatus("Deselected first marble for Jack card.");
        } else if (secondMarble == marble) {
            // Deselect second marble if clicking it again
            secondMarble = null;
            view.highlightCell(zone, position, false);
            view.updateStatus("Deselected second marble for Jack card.");
        }
    }

    private String findMarbleZone(Marble marble) {
        if (marble == null) return null;
        
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
        if (zone == null || marble == null) return -1;

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
        if (currentPlayerIndex < game.getPlayers().size() && 
            game.getPlayers().get(currentPlayerIndex) instanceof CPU) {
            isComputerTurn = true;
            view.updateStatus(game.getPlayers().get(currentPlayerIndex).getName() + "'s turn (thinking...)");
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
                                // CPU selects a card and marble randomly
                                Player cpuPlayer = game.getPlayers().get(currentPlayerIndex);
                                List<Card> availableCards = cpuPlayer.getHand();
                                if (!availableCards.isEmpty()) {
                                    // Randomly select a card
                                    Card selectedCard = availableCards.get(random.nextInt(availableCards.size()));
                                    game.selectCard(selectedCard);
                                    
                                    // Get actionable marbles for this card
                                    List<Marble> actionableMarbles = game.getBoard().getActionableMarbles().stream()
                                        .filter(m -> m.getColour() == cpuPlayer.getColour())
                                        .toList();
                                    
                                    // If Ace or King, consider fielding a marble
                                    if ((selectedCard.getName().equalsIgnoreCase("Ace") || 
                                         selectedCard.getName().equalsIgnoreCase("King")) &&
                                        random.nextBoolean()) {
                                        
                                        // Try to field a marble if possible
                                        List<Marble> homeMarbles = cpuPlayer.getMarbles();
                                        if (!homeMarbles.isEmpty()) {
                                            Marble marbleToField = homeMarbles.get(random.nextInt(homeMarbles.size()));
                                            if (game.canFieldMarble(marbleToField)) {
                                                // Animate highlighting the card and marble
                                                view.highlightCard(selectedCard, true);
                                                
                                                // Field the marble
                                                game.selectMarble(marbleToField);
                                                game.playPlayerTurn();
                                            }
                                        }
                                    } 
                                    // Regular movement
                                    else if (!actionableMarbles.isEmpty()) {
                                        // Randomly select a marble to move
                                        Marble selectedMarble = actionableMarbles.get(random.nextInt(actionableMarbles.size()));
                                        game.selectMarble(selectedMarble);
                                        
                                        // Handle Seven card
                                        if (selectedCard.toString().startsWith("7") && actionableMarbles.size() > 1 && random.nextBoolean()) {
                                            // Select another marble for split movement
                                            List<Marble> otherMarbles = actionableMarbles.stream()
                                                .filter(m -> m != selectedMarble)
                                                .toList();
                                            
                                            if (!otherMarbles.isEmpty()) {
                                                Marble secondMarble = otherMarbles.get(random.nextInt(otherMarbles.size()));
                                                game.selectSecondMarble(secondMarble);
                                                
                                                // Set random split distance
                                                int splitDistance = random.nextInt(6) + 1;
                                                game.editSplitDistance(splitDistance);
                                            }
                                        }
                                        
                                        // Animate highlighting the card and marble
                                        view.highlightCard(selectedCard, true);
                                        String zone = findMarbleZone(selectedMarble);
                                        int position = findMarblePosition(selectedMarble, zone);
                                        if (zone != null && position != -1) {
                                            view.highlightCell(zone, position, true);
                                        }
                                        
                                        // Short delay to show the selection
                                        Timer selectionTimer = new Timer();
                                        selectionTimer.schedule(new TimerTask() {
                                            @Override
                                            public void run() {
                                                Platform.runLater(() -> {
                                                    try {
                                                        // Play the turn
                                                        game.playPlayerTurn();
                                                    } catch (GameException e) {
                                                        showError("CPU turn error: " + e.getMessage());
                                                    }
                                                });
                                            }
                                        }, 500);
                                    }
                                }
                                
                                // End turn after a short delay
                                Timer endTurnTimer = new Timer();
                                endTurnTimer.schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        Platform.runLater(() -> {
                                            try {
                                                game.endPlayerTurn();
                                                currentPlayerIndex = (currentPlayerIndex + 1) % game.getPlayers().size();
                                                updateGameView();
                                                checkWinCondition();
                                                isComputerTurn = false;
                                                view.getPlayButton().setDisable(false);
                                                view.getCancelButton().setDisable(false);
                                                view.showComputerTurnProgress(false);
                                                
                                                // Check if next player is also CPU
                                                if (game.getPlayers().get(currentPlayerIndex) instanceof CPU) {
                                                    startComputerTurn();
                                                } else {
                                                    view.updateStatus("Your turn");
                                                    showValidActionsForCurrentPlayer();
                                                }
                                            } catch (GameException e) {
                                                showError(e.getMessage());
                                            }
                                        });
                                    }
                                }, 1000);
                                
                            } catch (GameException e) {
                                showError("CPU turn error: " + e.getMessage());
                                isComputerTurn = false;
                                view.getPlayButton().setDisable(false);
                                view.getCancelButton().setDisable(false);
                                view.showComputerTurnProgress(false);
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
        view.updatePlayerInfo(currentPlayer.getName(), currentPlayer.getColour(), game.getTurnCount());
        view.updateUserAreaName(currentPlayer.getName());
        
        // Update next player info
        int nextIndex = (currentPlayerIndex + 1) % game.getPlayers().size();
        Player nextPlayer = game.getPlayers().get(nextIndex);
        view.updateNextPlayer(nextPlayer.getName(), nextPlayer.getColour());
        
        // Update card display for human player
        if (!(currentPlayer instanceof CPU)) {
            view.updateCards(currentPlayer.getHand());
        } else {
            view.updateCards(new ArrayList<>());
        }
        
        // Update all marble positions
        updateAllMarblePositions();
    }
    
    private void updateAllMarblePositions() {
        // Clear all cells first
        for (String cellKey : view.cellViews.keySet()) {
            String[] parts = cellKey.split("_");
            if (parts.length == 2) {
                view.updateMarble(parts[0], Integer.parseInt(parts[1]), null);
            }
        }
        
        // Update track marbles
        ArrayList<Cell> track = game.getBoard().getTrack();
        for (int i = 0; i < track.size(); i++) {
            Marble marble = track.get(i).getMarble();
            if (marble != null) {
                view.updateMarble("track", i, marble.getColour());
            }
        }
        
        // Update safe zone marbles
        for (Colour colour : Colour.values()) {
            ArrayList<Cell> safeZone = game.getBoard().getSafeZones().stream()
                .filter(sz -> sz.getColour() == colour)
                .findFirst()
                .map(sz -> sz.getCells())
                .orElse(new ArrayList<>());
                
            for (int i = 0; i < safeZone.size(); i++) {
                Marble marble = safeZone.get(i).getMarble();
                if (marble != null) {
                    view.updateMarble("safe", i, marble.getColour());
                }
            }
        }
        
        // Update home marbles
        for (int i = 0; i < game.getPlayers().size(); i++) {
            Player player = game.getPlayers().get(i);
            List<Marble> homeMarbles = player.getMarbles();
            for (int j = 0; j < homeMarbles.size(); j++) {
                view.updateMarble("home", i * 4 + j, player.getColour());
            }
        }
    }

    private void animateCardPlay(Card card) {
        // Find the card view
        for (javafx.scene.Node node : view.getCardArea().getChildren()) {
            if (node instanceof javafx.scene.layout.HBox) {
                javafx.scene.layout.HBox cardRow = (javafx.scene.layout.HBox) node;
                for (javafx.scene.Node cardNode : cardRow.getChildren()) {
                    if (cardNode instanceof javafx.scene.layout.VBox) {
                        javafx.scene.layout.VBox cardBox = (javafx.scene.layout.VBox) cardNode;
                        if (cardBox.getUserData() == card) {
                            // Create scale animation
                            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), cardBox);
                            scaleDown.setToX(0.8);
                            scaleDown.setToY(0.8);
                            
                            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), cardBox);
                            scaleUp.setToX(1.0);
                            scaleUp.setToY(1.0);
                            
                            // Create fade animation
                            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), cardBox);
                            fadeOut.setFromValue(1.0);
