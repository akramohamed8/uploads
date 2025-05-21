package view;

import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.ProgressBar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import model.Colour;
import model.card.Card;

public class GameView {
    private BorderPane root;
    private GridPane boardGrid;
    private VBox playerInfo;
    private Label statusLabel;
    private HBox cardArea;
    private VBox userArea;
    private Button playButton;
    private Button cancelButton;
    private ComboBox<String> splitDistanceCombo;
    private TextField splitDistanceField;
    private ProgressBar computerTurnProgress;
    private BiConsumer<String, Integer> cellClickListener;
    private Consumer<Card> cardClickListener;
    
    // Map to store cell views for easy access
    public Map<String, StackPane> cellViews = new HashMap<>();
    
    // Color mapping
    private final Map<Colour, javafx.scene.paint.Color> colorMapping = Map.of(
        Colour.RED, javafx.scene.paint.Color.RED,
        Colour.BLUE, javafx.scene.paint.Color.BLUE,
        Colour.GREEN, javafx.scene.paint.Color.GREEN,
        Colour.YELLOW, javafx.scene.paint.Color.YELLOW
    );

    public GameView() {
        initializeView();
    }
    
    private void initializeView() {
        // Create main layout
        root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #f0f0f0;");
        
        // Create board
        createBoardLayout();
        
        // Create player info area
        createPlayerInfoArea();
        
        // Create user interaction area
        createUserArea();
        
        // Set up layout
        root.setCenter(boardGrid);
        root.setRight(playerInfo);
        root.setBottom(userArea);
        
        // Status label
        statusLabel = new Label("Game starting...");
        statusLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        statusLabel.setPadding(new Insets(5));
        root.setTop(statusLabel);
    }
    
    private void createBoardLayout() {
        boardGrid = new GridPane();
        boardGrid.setHgap(5);
        boardGrid.setVgap(5);
        boardGrid.setPadding(new Insets(10));
        boardGrid.setStyle("-fx-background-color: #e0e0e0; -fx-border-color: #999; -fx-border-width: 2px;");
        
        // Create the track (100 cells)
        createTrack();
        
        // Create home zones
        createHomeZones();
        
        // Create safe zones
        createSafeZones();
        
        // Create fire pit
        createFirePit();
    }
    
    private void createTrack() {
        // Create track cells in a loop that forms a rectangular path
        int trackSize = 100;
        int rows = 15;
        int cols = 15;
        
        // Calculate positions for cells
        for (int i = 0; i < trackSize; i++) {
            int row, col;
            
            // Top row (left to right)
            if (i < cols) {
                row = 0;
                col = i;
            }
            // Right column (top to bottom)
            else if (i < cols + rows - 1) {
                row = i - cols + 1;
                col = cols - 1;
            }
            // Bottom row (right to left)
            else if (i < 2 * cols + rows - 2) {
                row = rows - 1;
                col = 2 * cols + rows - 3 - i;
            }
            // Left column (bottom to top)
            else {
                row = 3 * cols + 2 * rows - 4 - i;
                col = 0;
            }
            
            StackPane cell = createCell("track", i);
            
            // Highlight base cells (positions 0, 25, 50, 75)
            if (i == 0 || i == 25 || i == 50 || i == 75) {
                cell.setStyle("-fx-background-color: #ffe699; -fx-border-color: #666; -fx-border-width: 2;");
                Label baseLabel = new Label("BASE");
                baseLabel.setFont(Font.font("System", FontWeight.BOLD, 10));
                cell.getChildren().add(baseLabel);
            } 
            // Highlight safe zone entry cells
            else if (i == 5 || i == 30 || i == 55 || i == 80) {
                cell.setStyle("-fx-background-color: #c6e2ff; -fx-border-color: #666; -fx-border-width: 2;");
                Label entryLabel = new Label("ENTRY");
                entryLabel.setFont(Font.font("System", FontWeight.BOLD, 10));
                cell.getChildren().add(entryLabel);
            }
            // Regular track cell
            else {
                cell.setStyle("-fx-background-color: white; -fx-border-color: #666; -fx-border-width: 1;");
            }
            
            // Add cell number label
            Label posLabel = new Label(String.valueOf(i));
            posLabel.setFont(Font.font("System", 8));
            posLabel.setTranslateY(-15); // Position above the cell
            cell.getChildren().add(posLabel);
            
            boardGrid.add(cell, col, row);
            cellViews.put("track_" + i, cell);
        }
    }
    
    private void createHomeZones() {
        // Create four home zones (one for each player)
        Colour[] colours = {Colour.RED, Colour.BLUE, Colour.GREEN, Colour.YELLOW};
        String[] positions = {"top-left", "top-right", "bottom-right", "bottom-left"};

        for (int i = 0; i < 4; i++) {
            VBox homeZone = new VBox(5);
            homeZone.setPadding(new Insets(10));
            homeZone.setStyle("-fx-background-color: " + toHexString(colorMapping.get(colours[i])) + "30; " +
                             "-fx-border-color: " + toHexString(colorMapping.get(colours[i])) + "; " +
                             "-fx-border-width: 2; -fx-border-radius: 5;");
            
            Label homeLabel = new Label(colours[i] + " HOME");
            homeLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            homeZone.getChildren().add(homeLabel);
            
            // Add 4 marble placeholders per home zone
            HBox marbleRow = new HBox(5);
            marbleRow.setAlignment(Pos.CENTER);
            
            for (int j = 0; j < 4; j++) {
                StackPane cell = createCell("home", i * 4 + j);
                cell.setStyle("-fx-background-color: " + toHexString(colorMapping.get(colours[i])) + "20; " +
                             "-fx-border-color: " + toHexString(colorMapping.get(colours[i])) + "; " +
                             "-fx-border-width: 1;");
                
                // Add the marble initially
                Circle marble = new Circle(15);
                marble.setFill(colorMapping.get(colours[i]));
                marble.setStroke(Color.BLACK);
                marble.setStrokeWidth(1);
                
                // Add effect
                DropShadow dropShadow = new DropShadow();
                dropShadow.setColor(Color.BLACK);
                dropShadow.setRadius(5);
                marble.setEffect(dropShadow);
                
                cell.getChildren().add(marble);
                marbleRow.getChildren().add(cell);
            }
            
            homeZone.getChildren().add(marbleRow);
            
            // Position home zones around the board
            switch (positions[i]) {
                case "top-left":
                    boardGrid.add(homeZone, 0, 3, 3, 3);
                    break;
                case "top-right":
                    boardGrid.add(homeZone, 12, 3, 3, 3);
                    break;
                case "bottom-right":
                    boardGrid.add(homeZone, 12, 9, 3, 3);
                    break;
                case "bottom-left":
                    boardGrid.add(homeZone, 0, 9, 3, 3);
                    break;
            }
        }
    }
    
    private void createSafeZones() {
        // Create four safe zones
        Colour[] colours = {Colour.RED, Colour.BLUE, Colour.GREEN, Colour.YELLOW};
        int[] startRows = {4, 4, 10, 10};
        int[] startCols = {4, 10, 10, 4};
        int[] rowIncrements = {0, 1, 0, -1};
        int[] colIncrements = {1, 0, -1, 0};
        
        for (int i = 0; i < 4; i++) {
            VBox safeZoneLabel = new VBox();
            safeZoneLabel.setAlignment(Pos.CENTER);
            Label label = new Label(colours[i] + " SAFE");
            label.setFont(Font.font("System", FontWeight.BOLD, 12));
            label.setTextFill(colorMapping.get(colours[i]));
            safeZoneLabel.getChildren().add(label);
            
            // Position labels
            if (i == 0) {
                boardGrid.add(safeZoneLabel, 7, 3, 1, 1);
            } else if (i == 1) {
                boardGrid.add(safeZoneLabel, 11, 7, 1, 1);
            } else if (i == 2) {
                boardGrid.add(safeZoneLabel, 7, 11, 1, 1);
            } else {
                boardGrid.add(safeZoneLabel, 3, 7, 1, 1);
            }
            
            // Create safe zone cells
            int row = startRows[i];
            int col = startCols[i];
            for (int j = 0; j < 4; j++) {
                StackPane cell = createCell("safe", j);
                cell.setStyle("-fx-background-color: " + toHexString(colorMapping.get(colours[i])) + "50; " +
                             "-fx-border-color: " + toHexString(colorMapping.get(colours[i])) + "; " +
                             "-fx-border-width: 1;");
                
                boardGrid.add(cell, col, row);
                cellViews.put("safe_" + j + "_" + colours[i], cell);
                
                row += rowIncrements[i];
                col += colIncrements[i];
            }
        }
    }
    
    private void createFirePit() {
        // Create fire pit in the center
        VBox firePit = new VBox(5);
        firePit.setAlignment(Pos.CENTER);
        firePit.setPadding(new Insets(10));
        firePit.setStyle("-fx-background-color: #ffcccc; -fx-border-color: #ff0000; -fx-border-width: 2; -fx-border-radius: 5;");
        
        Label firePitLabel = new Label("FIRE PIT");
        firePitLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        firePitLabel.setTextFill(Color.RED);
        
        // Stack of cards image
        StackPane cardStack = new StackPane();
        cardStack.setMinSize(80, 100);
        cardStack.setStyle("-fx-background-color: #ffffff; -fx-border-color: #000000; -fx-border-width: 1;");
        
        // Create visual indication of cards in the pit
        for (int i = 0; i < 3; i++) {
            Rectangle card = new Rectangle(70 - i*5, 90 - i*5);
            card.setFill(Color.WHITE);
            card.setStroke(Color.BLACK);
            card.setTranslateX(i * 2);
            card.setTranslateY(i * 2);
            cardStack.getChildren().add(card);
        }
        
        firePit.getChildren().addAll(firePitLabel, cardStack);
        boardGrid.add(firePit, 6, 6, 3, 3);
    }
    
    private void createPlayerInfoArea() {
        playerInfo = new VBox(10);
        playerInfo.setPadding(new Insets(10));
        playerInfo.setPrefWidth(200);
        playerInfo.setStyle("-fx-background-color: #e0e0e0; -fx-border-color: #999; -fx-border-width: 1;");
        
        Label infoHeader = new Label("Player Information");
        infoHeader.setFont(Font.font("System", FontWeight.BOLD, 16));
        
        Label currentPlayerLabel = new Label("Current Player: ");
        currentPlayerLabel.setId("currentPlayerLabel");
        
        Label turnCountLabel = new Label("Turn: 1");
        turnCountLabel.setId("turnCountLabel");
        
        Label nextPlayerLabel = new Label("Next Player: ");
        nextPlayerLabel.setId("nextPlayerLabel");
        
        // Computer turn progress indicator
        computerTurnProgress = new ProgressBar(0);
        computerTurnProgress.setPrefWidth(180);
        computerTurnProgress.setVisible(false);
        
        Separator separator = new Separator();
        
        Label cardDescriptionHeader = new Label("Card Description");
        cardDescriptionHeader.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        TextArea cardDescription = new TextArea();
        cardDescription.setId("cardDescription");
        cardDescription.setEditable(false);
        cardDescription.setPrefHeight(150);
        cardDescription.setWrapText(true);
        
        playerInfo.getChildren().addAll(
            infoHeader, currentPlayerLabel, turnCountLabel, nextPlayerLabel, 
            computerTurnProgress, separator, cardDescriptionHeader, cardDescription
        );
    }
    
    private void createUserArea() {
        userArea = new VBox(10);
        userArea.setPadding(new Insets(10));
        userArea.setStyle("-fx-background-color: #e0e0e0; -fx-border-color: #999; -fx-border-width: 1;");
        
        Label userHeader = new Label("Your Turn");
        userHeader.setId("userHeader");
        userHeader.setFont(Font.font("System", FontWeight.BOLD, 16));
        
        // Card area
        Label cardsLabel = new Label("Your Cards:");
        cardsLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        cardArea = new HBox(10);
        cardArea.setAlignment(Pos.CENTER);
        
        // Controls area
        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER);
        
        // Split distance for Seven card
        VBox splitDistanceBox = new VBox(5);
        splitDistanceBox.setAlignment(Pos.CENTER);
        Label splitLabel = new Label("Split Distance:");
        
        // Use combo box for split distance
        splitDistanceCombo = new ComboBox<>();
        for (int i = 1; i <= 6; i++) {
            splitDistanceCombo.getItems().add(String.valueOf(i));
        }
        splitDistanceCombo.setVisible(false);
        
        // Alternative: text field
        splitDistanceField = new TextField();
        splitDistanceField.setPromptText("1-6");
        splitDistanceField.setPrefWidth(60);
        splitDistanceField.setVisible(false);
        
        splitDistanceBox.getChildren().addAll(splitLabel, splitDistanceCombo);
        
        // Buttons
        playButton = new Button("Play");
        playButton.setPrefWidth(100);
        playButton.setStyle("-fx-background-color: #66cc66; -fx-text-fill: white; -fx-font-weight: bold;");
        
        cancelButton = new Button("Clear");
        cancelButton.setPrefWidth(100);
        cancelButton.setStyle("-fx-background-color: #ff6666; -fx-text-fill: white; -fx-font-weight: bold;");
        
        controls.getChildren().addAll(splitDistanceBox, playButton, cancelButton);
        
        userArea.getChildren().addAll(userHeader, cardsLabel, cardArea, controls);
    }
    
    private StackPane createCell(String zone, int position) {
        StackPane cell = new StackPane();
        cell.setMinSize(40, 40);
        cell.setUserData(zone + "_" + position); // Store position info
        
        // Add click event handler
        cell.setOnMouseClicked(event -> {
            if (cellClickListener != null) {
                cellClickListener.accept(zone, position);
            }
        });
        
        return cell;
    }
    
    // Helper method to convert javafx.scene.paint.Color to hex string
    private String toHexString(javafx.scene.paint.Color color) {
        return String.format("#%02X%02X%02X",
            (int) (color.getRed() * 255),
            (int) (color.getGreen() * 255),
            (int) (color.getBlue() * 255));
    }
    
    // Update methods
    public void updatePlayerInfo(String playerName, Colour colour, int turnCount) {
        Label currentPlayerLabel = (Label) findNodeById(playerInfo, "currentPlayerLabel");
        if (currentPlayerLabel != null) {
            currentPlayerLabel.setText("Current Player: " + playerName);
            currentPlayerLabel.setTextFill(colorMapping.get(colour));
        }
        
        Label turnCountLabel = (Label) findNodeById(playerInfo, "turnCountLabel");
        if (turnCountLabel != null) {
            turnCountLabel.setText("Turn: " + turnCount);
        }
    }
    
    public void updateNextPlayer(String playerName, Colour colour) {
        Label nextPlayerLabel = (Label) findNodeById(playerInfo, "nextPlayerLabel");
        if (nextPlayerLabel != null) {
            nextPlayerLabel.setText("Next Player: " + playerName);
            nextPlayerLabel.setTextFill(colorMapping.get(colour));
        }
    }
    
    public void updateUserAreaName(String name) {
        Label userHeader = (Label) findNodeById(userArea, "userHeader");
        if (userHeader != null) {
            userHeader.setText(name + "'s Turn");
        }
    }
    
    public void updateStatus(String status) {
        statusLabel.setText(status);
    }
    
    public void updateCards(List<Card> cards) {
        cardArea.getChildren().clear();
        
        if (cards.isEmpty()) {
            return;
        }
        
        // Create up to 4 card views
        for (Card card : cards) {
            VBox cardView = createCardView(card);
            cardArea.getChildren().add(cardView);
        }
    }
    
    private VBox createCardView(Card card) {
        VBox cardBox = new VBox(5);
        cardBox.setAlignment(Pos.CENTER);
        cardBox.setPadding(new Insets(5));
        cardBox.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 1; -fx-border-radius: 5;");
        cardBox.setMinSize(80, 120);
        cardBox.setMaxSize(80, 120);
        cardBox.setUserData(card); // Store the card object for reference
        
        Label rankLabel = new Label(card.getName());
        rankLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        
        Label suitLabel = new Label(card.getSuit().toString());
        String suitColor = (card.getSuit().toString().equals("HEARTS") || card.getSuit().toString().equals("DIAMONDS")) ? 
                          "red" : "black";
        suitLabel.setTextFill(Color.web(suitColor));
        
        cardBox.getChildren().addAll(rankLabel, suitLabel);
        
        // Add click handler
        cardBox.setOnMouseClicked(event -> {
            if (cardClickListener != null) {
                cardClickListener.accept(card);
            }
        });
        
        return cardBox;
    }
    
    public void updateCardDescription(Card card) {
        TextArea cardDescription = (TextArea) findNodeById(playerInfo, "cardDescription");
        if (cardDescription != null) {
            if (card == null) {
                cardDescription.setText("");
                return;
            }
            
            StringBuilder description = new StringBuilder();
            description.append(card.toString()).append("\n\n");
            
            // Add description based on card type
            switch (card.getName()) {
                case "Ace":
                    description.append("Field a marble from home to your base cell OR move 1 space forward.");
                    break;
                case "King":
                    description.append("Field a marble from home to your base cell OR move 13 spaces forward. Destroys any marbles in its path.");
                    break;
                case "Queen":
                    description.append("Move 12 spaces forward OR select a random opponent to skip their next turn.");
                    break;
                case "Jack":
                    description.append("Swap positions with an opponent's marble OR move 11 spaces forward.");
                    break;
                case "Four":
                    description.append("Move 4 spaces backward.");
                    break;
                case "Five":
                    description.append("Move any marble (yours or opponent's) 5 spaces forward.");
                    break;
                case "Seven":
                    description.append("Split 7 spaces between two marbles OR move 7 spaces forward with one marble.");
                    break;
                case "Ten":
                    description.append("Move 10 spaces forward OR force the next player to skip their turn.");
                    break;
                default:
                    if (card.getName().contains("Burner")) {
                        description.append("Send an opponent's marble back to home OR move according to card rank.");
                    } else if (card.getName().contains("Saver")) {
                        description.append("Move one of your marbles to your safe zone OR move according to card rank.");
                    } else {
                        description.append("Move forward by ").append(card.getValue()).append(" space(s).");
                    }
                    break;
            }
            
            cardDescription.setText(description.toString());
        }
    }
    
    public void updateMarble(String zone, int position, Colour colour) {
        String key = zone + "_" + position;
        if (zone.equals("safe")) {
            // For safe zones, we need to find the correct safe zone by color
            for (Colour c : Colour.values()) {
                String safeKey = "safe_" + position + "_" + c;
                if (cellViews.containsKey(safeKey)) {
                    StackPane cell = cellViews.get(safeKey);
                    updateMarbleInCell(cell, colour);
                }
            }
        } else if (cellViews.containsKey(key)) {
            StackPane cell = cellViews.get(key);
            updateMarbleInCell(cell, colour);
        }
    }
    
    private void updateMarbleInCell(StackPane cell, Colour colour) {
        // Remove any existing marble
        cell.getChildren().removeIf(node -> node instanceof Circle);
        
        if (colour != null) {
            // Add new marble
            Circle marble = new Circle(15);
            marble.setFill(colorMapping.get(colour));
            marble.setStroke(Color.BLACK);
            marble.setStrokeWidth(1);
            
            // Add drop shadow for 3D effect
            DropShadow dropShadow = new DropShadow();
            dropShadow.setColor(Color.BLACK);
            dropShadow.setRadius(5);
            marble.setEffect(dropShadow);
            
            cell.getChildren().add(marble);
        }
    }
    
    public void highlightCell(String zone, int position, boolean highlight) {
        String key = zone + "_" + position;
        
        if (zone.equals("safe")) {
            // For safe zones, try to find by current player's color
            for (Colour c : Colour.values()) {
                String safeKey = "safe_" + position + "_" + c;
                if (cellViews.containsKey(safeKey)) {
                    StackPane cell = cellViews.get(safeKey);
                    if (highlight) {
                        // Add glow effect
                        Glow glow = new Glow(0.8);
                        cell.setEffect(glow);
                        cell.setStyle(cell.getStyle() + "; -fx-border-color: yellow; -fx-border-width: 2;");
                    } else {
                        cell.setEffect(null);
                        // Remove yellow border but keep original style
                        String style = cell.getStyle().replace("; -fx-border-color: yellow; -fx-border-width: 2", "");
                        cell.setStyle(style);
                    }
                }
            }
        } else if (cellViews.containsKey(key)) {
            StackPane cell = cellViews.get(key);
            if (highlight) {
                // Add glow effect
                Glow glow = new Glow(0.8);
                cell.setEffect(glow);
                cell.setStyle(cell.getStyle() + "; -fx-border-color: yellow; -fx-border-width: 2;");
            } else {
                cell.setEffect(null);
                // Remove yellow border but keep original style
                String style = cell.getStyle().replace("; -fx-border-color: yellow; -fx-border-width: 2", "");
                cell.setStyle(style);
            }
        }
    }
    
    public void highlightCard(Card card, boolean highlight) {
        for (Node node : cardArea.getChildren()) {
            if (node instanceof VBox) {
                VBox cardBox = (VBox) node;
                if (cardBox.getUserData() == card) {
                    if (highlight) {
                        // Add glow effect
                        Glow glow = new Glow(0.8);
                        cardBox.setEffect(glow);
                        cardBox.setStyle(cardBox.getStyle() + "; -fx-border-color: yellow; -fx-border-width: 2;");
                    } else {
                        cardBox.setEffect(null);
                        // Remove yellow border but keep original style
                        String style = cardBox.getStyle().replace("; -fx-border-color: yellow; -fx-border-width: 2", "");
                        cardBox.setStyle(style);
                    }
                    break;
                }
            }
        }
    }
    
    public void highlightValidMoves(List<String> validMoves) {
        // Clear all highlights first
        for (StackPane cell : cellViews.values()) {
            String style = cell.getStyle().replace("; -fx-background-color: rgba(0,255,0,0.3)", "");
            cell.setStyle(style);
        }
        
        // Highlight valid moves
        for (String move : validMoves) {
            String[] parts = move.split("_");
            if (parts.length >= 2) {
                String zone = parts[0];
                int position = Integer.parseInt(parts[1]);
                
                String key = zone + "_" + position;
                
                if (zone.equals("safe")) {
                    // For safe zones, highlight in all colors
                    for (Colour c : Colour.values()) {
                        String safeKey = "safe_" + position + "_" + c;
                        if (cellViews.containsKey(safeKey)) {
                            StackPane cell = cellViews.get(safeKey);
                            cell.setStyle(cell.getStyle() + "; -fx-background-color: rgba(0,255,0,0.3)");
                        }
                    }
                } else if (cellViews.containsKey(key)) {
                    StackPane cell = cellViews.get(key);
                    cell.setStyle(cell.getStyle() + "; -fx-background-color: rgba(0,255,0,0.3)");
                }
            }
        }
    }
    
    public void showSplitDistanceInput(boolean show) {
        splitDistanceCombo.setVisible(show);
        // Alternatively, use text field:
        // splitDistanceField.setVisible(show);
    }
    
    public String getSplitDistance() {
        if (splitDistanceCombo.isVisible()) {
            return splitDistanceCombo.getValue();
        }
        // If using text field:
        // if (splitDistanceField.isVisible()) {
        //     return splitDistanceField.getText();
        // }
        return null;
    }
    
    public void showComputerTurnProgress(boolean show) {
        computerTurnProgress.setVisible(show);
    }
    
    public void updateComputerTurnProgress(double progress) {
        computerTurnProgress.setProgress(progress);
    }
    
    // Animation methods
    public void animateMarbleMove(String fromZone, int fromPos, String toZone, int toPos, Colour colour) {
        // Find source and target cells
        StackPane sourceCell = null;
        StackPane targetCell = null;
        
        if (fromZone.equals("home")) {
            String key = fromZone + "_" + fromPos;
            sourceCell = cellViews.get(key);
        } else if (fromZone.equals("safe")) {
            String safeKey = "safe_" + fromPos + "_" + colour;
            sourceCell = cellViews.get(safeKey);
        } else {
            String key = fromZone + "_" + fromPos;
            sourceCell = cellViews.get(key);
        }
        
        if (toZone.equals("home")) {
            String key = toZone + "_" + toPos;
            targetCell = cellViews.get(key);
        } else if (toZone.equals("safe")) {
            String safeKey = "safe_" + toPos + "_" + colour;
            targetCell = cellViews.get(safeKey);
        } else {
            String key = toZone + "_" + toPos;
            targetCell = cellViews.get(key);
        }
        
        if (sourceCell == null || targetCell == null) return;
        
        // Create a marble for animation
        Circle marble = new Circle(15);
        marble.setFill(colorMapping.get(colour));
        marble.set
