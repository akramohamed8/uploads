package view;

import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.animation.*;
import javafx.util.Duration;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.Light;
import javafx.scene.effect.Lighting;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import javafx.scene.Scene;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Random;
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
    private Consumer<KeyEvent> keyEventHandler;
    private Stage mainStage;
    private List<Integer> trapCells = new ArrayList<>();
    private Random random = new Random();
    
    // Map to store cell views for easy access
    public Map<String, StackPane> cellViews = new HashMap<>();
    
    // Track selected elements
    private Card selectedCard;
    private List<String> selectedMarbles = new ArrayList<>();
    
    // Color mapping
    private final Map<Colour, javafx.scene.paint.Color> colorMapping = Map.of(
        Colour.RED, javafx.scene.paint.Color.RED,
        Colour.BLUE, javafx.scene.paint.Color.BLUE,
        Colour.GREEN, javafx.scene.paint.Color.GREEN,
        Colour.YELLOW, javafx.scene.paint.Color.YELLOW
    );

    public GameView() {
        initializeView();
        initializeTrapCells();
    }
    
    public void setMainStage(Stage stage) {
        this.mainStage = stage;
        
        // Add key event handler to the scene
        Scene scene = mainStage.getScene();
        if (scene != null) {
            scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.F) {
                    // Trigger field action when 'F' is pressed
                    if (keyEventHandler != null) {
                        keyEventHandler.accept(event);
                    }
                }
            });
        }
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
    
    private void initializeTrapCells() {
        // Randomly place trap cells on the track (avoid base cells and safe zone entries)
        List<Integer> invalidPositions = List.of(0, 5, 25, 30, 50, 55, 75, 80);
        
        // Create 8 trap cells
        while (trapCells.size() < 8) {
            int position = random.nextInt(100);
            if (!invalidPositions.contains(position) && !trapCells.contains(position)) {
                trapCells.add(position);
                // Mark trap cells visually
                markTrapCell(position, true);
            }
        }
    }
    
    private void markTrapCell(int position, boolean isTrap) {
        String key = "track_" + position;
        if (cellViews.containsKey(key)) {
            StackPane cell = cellViews.get(key);
            if (isTrap) {
                // Subtle indicator for trap cell
                Rectangle trapMarker = new Rectangle(10, 10);
                trapMarker.setFill(Color.RED);
                trapMarker.setOpacity(0.3);
                trapMarker.getStyleClass().add("trap-marker");
                
                // Position the marker in the bottom-right corner
                StackPane.setAlignment(trapMarker, Pos.BOTTOM_RIGHT);
                cell.getChildren().add(trapMarker);
            } else {
                // Remove trap marker
                cell.getChildren().removeIf(node -> node.getStyleClass().contains("trap-marker"));
            }
        }
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
                
                // Store cell view for reference
                cellViews.put("home_" + (i * 4 + j), cell);
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
        
        // Selection info
        Label selectionHeader = new Label("Current Selection");
        selectionHeader.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        Label selectionInfo = new Label("No card or marbles selected");
        selectionInfo.setId("selectionInfo");
        selectionInfo.setWrapText(true);
        
        // Keyboard shortcuts info
        Label shortcutsHeader = new Label("Keyboard Shortcuts");
        shortcutsHeader.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        Label shortcutsInfo = new Label("F - Field a marble using Ace or King");
        shortcutsInfo.setWrapText(true);
        
        playerInfo.getChildren().addAll(
            infoHeader, currentPlayerLabel, turnCountLabel, nextPlayerLabel, 
            computerTurnProgress, separator, cardDescriptionHeader, cardDescription,
            selectionHeader, selectionInfo, shortcutsHeader, shortcutsInfo
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
                
                // Update selection for this cell
                String key = zone + "_" + position;
                if (selectedMarbles.contains(key)) {
                    selectedMarbles.remove(key);
                    highlightCell(zone, position, false);
                } else {
                    selectedMarbles.add(key);
                    highlightCell(zone, position, true);
                }
                
                // Update selection info
                updateSelectionInfo();
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
        
        // Reset selected card
        selectedCard = null;
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
                
                // Handle selection logic
                if (selectedCard == card) {
                    // Deselect the card
                    selectedCard = null;
                    highlightCard(card, false);
                } else {
                    // If there was a previously selected card, deselect it
                    if (selectedCard != null) {
                        highlightCard(selectedCard, false);
                    }
                    
                    // Select the new card
                    selectedCard = card;
                    highlightCard(card, true);
                    
                    // Show split distance input if it's a Seven card
                    showSplitDistanceInput(card.getName().equals("Seven"));
                }
                
                // Update card description
                updateCardDescription(selectedCard);
                
                // Update selection info
                updateSelectionInfo();
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
    
    public void updateSelectionInfo() {
        Label selectionInfo = (Label) findNodeById(playerInfo, "selectionInfo");
        if (selectionInfo != null) {
            StringBuilder info = new StringBuilder();
            
            if (selectedCard != null) {
                info.append("Selected Card: ").append(selectedCard.getName()).append(" of ").append(selectedCard.getSuit()).append("\n");
            } else {
                info.append("No card selected\n");
            }
            
            if (!selectedMarbles.isEmpty()) {
                info.append("Selected Marbles: ");
                for (int i = 0; i < selectedMarbles.size(); i++) {
                    String[] parts = selectedMarbles.get(i).split("_");
                    info.append(parts[0]).append(" ").append(parts[1]);
                    
                    if (i < selectedMarbles.size() - 1) {
                        info.append(", ");
                    }
                }
            } else {
                info.append("No marbles selected");
            }
            
            selectionInfo.setText(info.toString());
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
        // Create and add new marble
        Circle marble = new Circle(15);
        marble.setFill(colorMapping.get(colour));
        marble.setStroke(Color.BLACK);
        marble.setStrokeWidth(1);
        
        // Add effect
        DropShadow dropShadow = new DropShadow();
        dropShadow.setColor(Color.BLACK);
        dropShadow.setRadius(5);
        marble.setEffect(dropShadow);
        
        cell.getChildren().add(marble);
        
        // Add entrance animation
        ScaleTransition st = new ScaleTransition(Duration.millis(300), marble);
        st.setFromX(0.5);
        st.setFromY(0.5);
        st.setToX(1.0);
        st.setToY(1.0);
        st.play();
    }
}

public void moveMarble(String fromZone, int fromPosition, String toZone, int toPosition, Colour colour) {
    // Remove marble from source
    updateMarble(fromZone, fromPosition, null);
    
    // Add marble to destination with animation
    String toKey = toZone + "_" + toPosition;
    if (toZone.equals("safe")) {
        // For safe zones, we need to find the correct safe zone by color
        toKey = "safe_" + toPosition + "_" + colour;
    }
    
    if (cellViews.containsKey(toKey)) {
        StackPane toCell = cellViews.get(toKey);
        
        // Create marble for animation
        Circle marble = new Circle(15);
        marble.setFill(colorMapping.get(colour));
        marble.setStroke(Color.BLACK);
        marble.setStrokeWidth(1);
        
        // Add effect
        DropShadow dropShadow = new DropShadow();
        dropShadow.setColor(Color.BLACK);
        dropShadow.setRadius(5);
        marble.setEffect(dropShadow);
        
        // Get position info for animation
        double startX = toCell.getLayoutX() - 100; // Offset for animation
        double startY = toCell.getLayoutY();
        double endX = toCell.getLayoutX();
        double endY = toCell.getLayoutY();
        
        // Add marble temporarily for animation
        root.getChildren().add(marble);
        marble.setTranslateX(startX);
        marble.setTranslateY(startY);
        
        // Create and play animation
        TranslateTransition tt = new TranslateTransition(Duration.millis(500), marble);
        tt.setFromX(startX);
        tt.setFromY(startY);
        tt.setToX(endX);
        tt.setToY(endY);
        
        tt.setOnFinished(event -> {
            // Remove temporary marble
            root.getChildren().remove(marble);
            
            // Add permanent marble to destination
            updateMarble(toZone, toPosition, colour);
        });
        
        tt.play();
    } else {
        // If no animation can be performed, just update
        updateMarble(toZone, toPosition, colour);
    }
}

public void animateMarbleMovement(String fromZone, int fromPosition, String toZone, int toPosition, Colour colour) {
    // Get source and target cells
    String fromKey = fromZone + "_" + fromPosition;
    String toKey = toZone + "_" + toPosition;
    
    if (toZone.equals("safe")) {
        toKey = "safe_" + toPosition + "_" + colour;
    }
    
    StackPane fromCell = cellViews.get(fromKey);
    StackPane toCell = cellViews.get(toKey);
    
    if (fromCell == null || toCell == null) {
        // If cells not found, just perform direct update
        updateMarble(fromZone, fromPosition, null);
        updateMarble(toZone, toPosition, colour);
        return;
    }
    
    // Find marble in source cell
    Circle marble = null;
    for (Node node : fromCell.getChildren()) {
        if (node instanceof Circle) {
            marble = (Circle) node;
            break;
        }
    }
    
    if (marble == null) {
        // If no marble found, just perform direct update
        updateMarble(fromZone, fromPosition, null);
        updateMarble(toZone, toPosition, colour);
        return;
    }
    
    // Remove marble from source cell
    fromCell.getChildren().remove(marble);
    
    // Calculate start and end positions in scene coordinates
    Bounds fromBounds = fromCell.localToScene(fromCell.getBoundsInLocal());
    Bounds toBounds = toCell.localToScene(toCell.getBoundsInLocal());
    
    // Add marble to root for animation
    root.getChildren().add(marble);
    marble.setTranslateX(fromBounds.getCenterX());
    marble.setTranslateY(fromBounds.getCenterY());
    
    // Create and play animation
    TranslateTransition tt = new TranslateTransition(Duration.millis(700), marble);
    tt.setToX(toBounds.getCenterX());
    tt.setToY(toBounds.getCenterY());
    
    // Add a small bounce effect
    tt.setInterpolator(Interpolator.SPLINE(0.1, 0.9, 0.2, 1.0));
    
    // Add glow effect during movement
    Glow glow = new Glow();
    glow.setLevel(0.7);
    marble.setEffect(glow);
    
    tt.setOnFinished(event -> {
        // Remove temporary marble
        root.getChildren().remove(marble);
        
        // Add permanent marble to destination
        updateMarble(toZone, toPosition, colour);
    });
    
    tt.play();
}

public void highlightCard(Card card, boolean highlight) {
    // Find the card view
    for (Node node : cardArea.getChildren()) {
        if (node instanceof VBox) {
            Card nodeCard = (Card) node.getUserData();
            if (nodeCard == card) {
                if (highlight) {
                    node.setStyle("-fx-background-color: #ffff99; -fx-border-color: #ffcc00; -fx-border-width: 2; -fx-border-radius: 5;");
                    
                    // Add glow effect
                    Glow glow = new Glow();
                    glow.setLevel(0.5);
                    node.setEffect(glow);
                } else {
                    node.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 1; -fx-border-radius: 5;");
                    node.setEffect(null);
                }
                break;
            }
        }
    }
}

public void highlightCell(String zone, int position, boolean highlight) {
    String key = zone + "_" + position;
    
    // For safe zones, try all colors
    if (zone.equals("safe")) {
        for (Colour colour : Colour.values()) {
            String safeKey = "safe_" + position + "_" + colour;
            if (cellViews.containsKey(safeKey)) {
                StackPane cell = cellViews.get(safeKey);
                highlightCellView(cell, highlight);
            }
        }
    } else if (cellViews.containsKey(key)) {
        StackPane cell = cellViews.get(key);
        highlightCellView(cell, highlight);
    }
}

private void highlightCellView(StackPane cell, boolean highlight) {
    if (highlight) {
        // Store original style
        if (cell.getUserData() instanceof String) {
            // Already has user data for position, so don't overwrite
            cell.getProperties().put("originalStyle", cell.getStyle());
        } else {
            cell.setUserData(cell.getStyle());
        }
        
        // Add highlight
        String currentStyle = cell.getStyle();
        cell.setStyle(currentStyle + "-fx-background-color: #ffff99; -fx-border-color: #ffcc00; -fx-border-width: 2;");
        
        // Add glow effect
        Glow glow = new Glow();
        glow.setLevel(0.5);
        cell.setEffect(glow);
    } else {
        // Restore original style
        String originalStyle = null;
        if (cell.getProperties().containsKey("originalStyle")) {
            originalStyle = (String) cell.getProperties().get("originalStyle");
        } else if (!(cell.getUserData() instanceof String)) {
            originalStyle = (String) cell.getUserData();
        }
        
        if (originalStyle != null) {
            cell.setStyle(originalStyle);
        }
        
        cell.setEffect(null);
    }
}

public void showSplitDistanceInput(boolean show) {
    splitDistanceCombo.setVisible(show);
    splitDistanceField.setVisible(false); // Use combo box by default
}

public String getSplitDistance() {
    if (splitDistanceCombo.isVisible()) {
        return splitDistanceCombo.getValue();
    } else if (splitDistanceField.isVisible()) {
        return splitDistanceField.getText();
    }
    return null;
}

public void setPlayButtonAction(Runnable action) {
    playButton.setOnAction(event -> action.run());
}

public void setCancelButtonAction(Runnable action) {
    cancelButton.setOnAction(event -> {
        // Clear selections
        selectedCard = null;
        selectedMarbles.clear();
        
        // Remove all highlights
        for (Node node : cardArea.getChildren()) {
            node.setStyle("-fx-background-color: white; -fx-border-color: black; -fx-border-width: 1; -fx-border-radius: 5;");
            node.setEffect(null);
        }
        
        for (StackPane cell : cellViews.values()) {
            cell.setEffect(null);
            // Only restore style if it was changed
            if (cell.getProperties().containsKey("originalStyle")) {
                cell.setStyle((String) cell.getProperties().get("originalStyle"));
                cell.getProperties().remove("originalStyle");
            }
        }
        
        // Update info
        updateCardDescription(null);
        updateSelectionInfo();
        
        // Hide split distance input
        showSplitDistanceInput(false);
        
        // Run additional actions
        action.run();
    });
}

public void setCellClickListener(BiConsumer<String, Integer> listener) {
    this.cellClickListener = listener;
}

public void setCardClickListener(Consumer<Card> listener) {
    this.cardClickListener = listener;
}

public void setKeyEventHandler(Consumer<KeyEvent> handler) {
    this.keyEventHandler = handler;
}

public Card getSelectedCard() {
    return selectedCard;
}

public List<String> getSelectedMarbles() {
    return new ArrayList<>(selectedMarbles);
}

public void clearSelections() {
    // Simulate cancel button click
    cancelButton.fire();
}

// Computer turn animation methods
public void showComputerTurnProgress(boolean show) {
    computerTurnProgress.setVisible(show);
}

public void setComputerTurnProgress(double progress) {
    computerTurnProgress.setProgress(progress);
}

public void animateComputerTurn(Runnable onComplete) {
    showComputerTurnProgress(true);
    
    Timeline timeline = new Timeline();
    timeline.getKeyFrames().add(
        new KeyFrame(Duration.ZERO, new KeyValue(computerTurnProgress.progressProperty(), 0)),
        new KeyFrame(Duration.seconds(1.5), new KeyValue(computerTurnProgress.progressProperty(), 1))
    );
    
    timeline.setOnFinished(event -> {
        showComputerTurnProgress(false);
        if (onComplete != null) {
            onComplete.run();
        }
    });
    
    timeline.play();
}

public void animateCardDiscard(Card card, Runnable onComplete) {
    // Find the card view
    VBox cardView = null;
    for (Node node : cardArea.getChildren()) {
        if (node instanceof VBox) {
            Card nodeCard = (Card) node.getUserData();
            if (nodeCard == card) {
                cardView = (VBox) node;
                break;
            }
        }
    }
    
    if (cardView == null) {
        if (onComplete != null) {
            onComplete.run();
        }
        return;
    }
    
    // Fire pit is in the center of the board
    Bounds firePitBounds = boardGrid.localToScene(boardGrid.getBoundsInLocal());
    double centerX = firePitBounds.getMinX() + firePitBounds.getWidth() / 2;
    double centerY = firePitBounds.getMinY() + firePitBounds.getHeight() / 2;
    
    // Card's current position
    Bounds cardBounds = cardView.localToScene(cardView.getBoundsInLocal());
    
    // Create an animation of the card flying to the fire pit
    TranslateTransition tt = new TranslateTransition(Duration.millis(800), cardView);
    tt.setToX(centerX - cardBounds.getMinX());
    tt.setToY(centerY - cardBounds.getMinY());
    
    // Add rotation
    RotateTransition rt = new RotateTransition(Duration.millis(800), cardView);
    rt.setToAngle(360);
    
    // Add fade out
    FadeTransition ft = new FadeTransition(Duration.millis(800), cardView);
    ft.setToValue(0);
    
    // Play animations together
    ParallelTransition pt = new ParallelTransition(tt, rt, ft);
    
    pt.setOnFinished(event -> {
        // Remove the card
        cardArea.getChildren().remove(cardView);
        
        // Call completion handler
        if (onComplete != null) {
            onComplete.run();
        }
    });
    
    pt.play();
}

public void animateTrapHit(int trackPosition, Runnable onComplete) {
    String key = "track_" + trackPosition;
    if (cellViews.containsKey(key)) {
        StackPane cell = cellViews.get(key);
        
        // Store original style
        String originalStyle = cell.getStyle();
        
        // Flash effect
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, e -> cell.setStyle("-fx-background-color: #ff6666; -fx-border-color: #ff0000; -fx-border-width: 2;")),
            new KeyFrame(Duration.millis(200), e -> cell.setStyle(originalStyle)),
            new KeyFrame(Duration.millis(400), e -> cell.setStyle("-fx-background-color: #ff6666; -fx-border-color: #ff0000; -fx-border-width: 2;")),
            new KeyFrame(Duration.millis(600), e -> cell.setStyle(originalStyle))
        );
        
        timeline.setOnFinished(event -> {
            if (onComplete != null) {
                onComplete.run();
            }
        });
        
        timeline.play();
    } else {
        if (onComplete != null) {
            onComplete.run();
        }
    }
}

public void showWinnerPopup(Colour winnerColour) {
    // Create alert dialog
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle("Game Over");
    alert.setHeaderText("We Have a Winner!");
    alert.setContentText("The " + winnerColour + " player has won the game!");
    
    // Make it look nice
    DialogPane dialogPane = alert.getDialogPane();
    dialogPane.setStyle("-fx-background-color: #f0f0f0;");
    
    // Add celebration effect (confetti-like particles)
    StackPane alertContent = new StackPane();
    alertContent.setPrefSize(400, 100);
    
    // Add some celebration effects
    for (int i = 0; i < 20; i++) {
        Rectangle confetti = new Rectangle(10, 10);
        confetti.setFill(Color.color(Math.random(), Math.random(), Math.random()));
        confetti.setOpacity(0.7);
        confetti.setX(Math.random() * 400);
        confetti.setY(Math.random() * 100);
        
        // Add animation
        TranslateTransition tt = new TranslateTransition(Duration.seconds(2), confetti);
        tt.setFromY(0);
        tt.setToY(100);
        tt.setCycleCount(Animation.INDEFINITE);
        tt.setAutoReverse(true);
        
        RotateTransition rt = new RotateTransition(Duration.seconds(3), confetti);
        rt.setToAngle(360);
        rt.setCycleCount(Animation.INDEFINITE);
        
        ParallelTransition pt = new ParallelTransition(confetti, tt, rt);
        pt.play();
        
        alertContent.getChildren().add(confetti);
    }
    
    alert.getDialogPane().setExpandableContent(alertContent);
    alert.getDialogPane().setExpanded(true);
    
    alert.showAndWait();
}

public void showAlert(String title, String message) {
    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
}

// Repositions trap cells randomly
public void repositionTrapCells() {
    // Clear old trap cells
    for (Integer position : trapCells) {
        markTrapCell(position, false);
    }
    
    trapCells.clear();
    
    // Add new trap cells
    List<Integer> invalidPositions = List.of(0, 5, 25, 30, 50, 55, 75, 80);
    
    // Create 8 trap cells
    while (trapCells.size() < 8) {
        int position = random.nextInt(100);
        if (!invalidPositions.contains(position) && !trapCells.contains(position)) {
            trapCells.add(position);
            // Mark trap cells visually
            markTrapCell(position, true);
        }
    }
}

private Node findNodeById(Parent parent, String id) {
    for (Node node : parent.getChildrenUnmodifiable()) {
        if (id.equals(node.getId())) {
            return node;
        }
        
        if (node instanceof Parent) {
            Node result = findNodeById((Parent) node, id);
            if (result != null) {
                return result;
            }
        }
    }
    
    return null;
}

public BorderPane getRoot() {
    return root;
}
}
