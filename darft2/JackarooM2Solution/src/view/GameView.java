package view;

import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import model.Colour;
import model.card.Card;
import model.player.Marble;
import engine.board.Board;
import engine.board.Cell;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class GameView {
    private final BorderPane root;
    private final Button playButton;
    private final Button cancelButton;
    public final Map<String, StackPane> cellViews;
    private final VBox cardArea;
    private final GridPane board;
    private final VBox playerInfo;
    private final VBox homeZone;
    private final VBox safeZone;
    private Label turnLabel;
    private Label playerLabel;
    private Label statusLabel;
    private Label nextPlayerLabel;
    private TextField splitDistanceField;
    private VBox splitDistanceBox;
    private ProgressBar computerTurnProgress;
    private TextArea cardDescription;
    private Label zoneLabel;
    private CellClickListener cellClickListener;
    private CardClickListener cardClickListener;

    public interface CellClickListener {
        void onCellClick(String zone, int position);
    }

    public interface CardClickListener {
        void onCardClick(Card card);
    }

    public void setCellClickListener(CellClickListener listener) {
        this.cellClickListener = listener;
    }

    public void setCardClickListener(CardClickListener listener) {
        this.cardClickListener = listener;
    }

    public GameView() {
        root = new BorderPane();
        root.setPadding(new Insets(20));
        
        // Initialize components
        playButton = new Button("Play Turn");
        cancelButton = new Button("Cancel");
        cellViews = new HashMap<>();
        cardArea = new VBox(10);
        board = new GridPane();
        playerInfo = new VBox(10);
        homeZone = new VBox(10);
        safeZone = new VBox(10);
        statusLabel = new Label("Status: Ready");
        nextPlayerLabel = new Label("Next Player: ");
        computerTurnProgress = new ProgressBar(0);
        computerTurnProgress.setVisible(false);
        cardDescription = new TextArea();
        cardDescription.setEditable(false);
        cardDescription.setWrapText(true);
        cardDescription.setPrefRowCount(3);
        zoneLabel = new Label("Board Zones");
        zoneLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        // Split distance input for Seven card
        splitDistanceBox = new VBox(5);
        splitDistanceField = new TextField();
        splitDistanceField.setPromptText("Enter split distance (1-6)");
        splitDistanceField.setMaxWidth(100);
        splitDistanceBox.getChildren().addAll(new Label("Split Distance:"), splitDistanceField);
        splitDistanceBox.setVisible(false);
        
        setupBoard();
        setupPlayerInfo();
        setupControls();
        setupLayout();
    }

    private void setupBoard() {
        // Use a Pane for absolute positioning
        Pane rectangularBoard = new Pane();
        rectangularBoard.setPrefSize(800, 800);
        rectangularBoard.setStyle("-fx-background-color: #bfa76f; -fx-border-color: #7a5c2e; -fx-border-width: 12px;");
        board.getChildren().clear();
        board.getChildren().add(rectangularBoard);

        double cellSize = 60;
        int trackCells = 40; // Number of cells on the track
        double boardWidth = 800;
        double boardHeight = 800;
        double trackWidth = boardWidth - 2 * cellSize;
        double trackHeight = boardHeight - 2 * cellSize;

        // Calculate positions for track cells
        for (int i = 0; i < trackCells; i++) {
            double x, y;
            String style = "-fx-background-color: #e6e2d3; -fx-border-color: #7a5c2e; -fx-border-width: 2px; -fx-background-radius: 20px; -fx-border-radius: 20px;";
            
            // Top row (left to right)
            if (i < 10) {
                x = cellSize + (i * (trackWidth / 10));
                y = cellSize;
            }
            // Right column (top to bottom)
            else if (i < 20) {
                x = boardWidth - cellSize;
                y = cellSize + ((i - 10) * (trackHeight / 10));
            }
            // Bottom row (right to left)
            else if (i < 30) {
                x = boardWidth - cellSize - ((i - 20) * (trackWidth / 10));
                y = boardHeight - cellSize;
            }
            // Left column (bottom to top)
            else {
                x = cellSize;
                y = boardHeight - cellSize - ((i - 30) * (trackHeight / 10));
            }

            // Add special styling for base and safe entry cells
            if (i % 10 == 0) {
                style += " -fx-border-color: #1e90ff; -fx-border-width: 3px;";
            }
            if (i % 10 == 8) {
                style += " -fx-border-color: #a020f0; -fx-border-width: 3px;";
            }

            StackPane cell = new StackPane();
            cell.setPrefSize(cellSize, cellSize);
            cell.setStyle(style);
            cellViews.put("track_" + i, cell);
            final int idx = i;
            cell.setOnMouseClicked(e -> {
                if (cellClickListener != null) cellClickListener.onCellClick("track", idx);
            });
            cell.setLayoutX(x);
            cell.setLayoutY(y);
            rectangularBoard.getChildren().add(cell);
        }

        // Draw home zones (corners)
        Colour[] homeColours = {Colour.BLUE, Colour.YELLOW, Colour.RED, Colour.GREEN};
        double[][] homePositions = {
            {cellSize, cellSize}, // Top-left
            {boardWidth - 2 * cellSize, cellSize}, // Top-right
            {boardWidth - 2 * cellSize, boardHeight - 2 * cellSize}, // Bottom-right
            {cellSize, boardHeight - 2 * cellSize} // Bottom-left
        };

        for (int i = 0; i < 4; i++) {
            StackPane home = new StackPane();
            home.setPrefSize(cellSize, cellSize);
            home.setStyle("-fx-background-color: " + toHex(convertColour(homeColours[i])) + "; -fx-border-color: #ff0000; -fx-border-width: 3px; -fx-background-radius: 20px; -fx-border-radius: 20px;");
            cellViews.put("home_" + i, home);
            final int idx = i;
            home.setOnMouseClicked(e -> {
                if (cellClickListener != null) cellClickListener.onCellClick("home", idx);
            });
            home.setLayoutX(homePositions[i][0]);
            home.setLayoutY(homePositions[i][1]);
            rectangularBoard.getChildren().add(home);
        }

        // Draw safe zones (one for each color)
        double safeZoneSize = 4 * cellSize;
        double[][] safeZonePositions = {
            {boardWidth/2 - safeZoneSize/2, cellSize}, // Top
            {boardWidth - 2 * cellSize, boardHeight/2 - safeZoneSize/2}, // Right
            {boardWidth/2 - safeZoneSize/2, boardHeight - 2 * cellSize}, // Bottom
            {cellSize, boardHeight/2 - safeZoneSize/2} // Left
        };

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                StackPane safeCell = new StackPane();
                safeCell.setPrefSize(cellSize, cellSize);
                safeCell.setStyle("-fx-background-color: #d0f5d0; -fx-border-color: #228b22; -fx-border-width: 3px; -fx-background-radius: 20px; -fx-border-radius: 20px;");
                cellViews.put("safe_" + (i * 4 + j), safeCell);
                final int idx = i * 4 + j;
                safeCell.setOnMouseClicked(e -> {
                    if (cellClickListener != null) cellClickListener.onCellClick("safe", idx);
                });
                
                // Position cells in a line based on their zone
                if (i == 0) { // Top
                    safeCell.setLayoutX(safeZonePositions[i][0] + j * cellSize);
                    safeCell.setLayoutY(safeZonePositions[i][1]);
                } else if (i == 1) { // Right
                    safeCell.setLayoutX(safeZonePositions[i][0]);
                    safeCell.setLayoutY(safeZonePositions[i][1] + j * cellSize);
                } else if (i == 2) { // Bottom
                    safeCell.setLayoutX(safeZonePositions[i][0] + j * cellSize);
                    safeCell.setLayoutY(safeZonePositions[i][1]);
                } else { // Left
                    safeCell.setLayoutX(safeZonePositions[i][0]);
                    safeCell.setLayoutY(safeZonePositions[i][1] + j * cellSize);
                }
                rectangularBoard.getChildren().add(safeCell);
            }
        }

        // Central firepit
        StackPane firepit = new StackPane();
        firepit.setPrefSize(200, 200);
        firepit.setStyle("-fx-background-color: #8b6f3e; -fx-border-color: #5a3c0a; -fx-border-width: 4px; -fx-background-radius: 10px; -fx-border-radius: 10px;");
        Label firepitLabel = new Label("Firepit");
        firepitLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 24px; -fx-text-fill: #fff;");
        firepit.getChildren().add(firepitLabel);
        firepit.setLayoutX(boardWidth/2 - 100);
        firepit.setLayoutY(boardHeight/2 - 100);
        rectangularBoard.getChildren().add(firepit);
    }

    private String toHex(javafx.scene.paint.Color color) {
        return String.format("#%02X%02X%02X", (int)(color.getRed()*255), (int)(color.getGreen()*255), (int)(color.getBlue()*255));
    }

    private void setupPlayerInfo() {
        turnLabel = new Label("Turn: 1");
        turnLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        playerLabel = new Label("Current Player: ");
        playerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        nextPlayerLabel = new Label("Next Player: ");
        nextPlayerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        statusLabel = new Label("Status: Ready");
        statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0066CC;");
        
        computerTurnProgress = new ProgressBar(0);
        computerTurnProgress.setVisible(false);
        computerTurnProgress.setStyle("-fx-accent: #0066CC;");
        
        VBox infoBox = new VBox(10);
        infoBox.setStyle("-fx-background-color: #F5F5F5; -fx-padding: 10px; -fx-border-color: #999999; -fx-border-width: 1px;");
        infoBox.getChildren().addAll(turnLabel, playerLabel, nextPlayerLabel, statusLabel, computerTurnProgress);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        
        playerInfo.getChildren().add(infoBox);
    }

    private void setupControls() {
        cardArea.setAlignment(Pos.CENTER);
        cardArea.setPadding(new Insets(10));
        cardArea.setStyle("-fx-border-color: #999999; -fx-border-width: 1px; -fx-background-color: #F5F5F5;");
        
        // Add a title for the card area
        Label cardTitle = new Label("Your Cards");
        cardTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        cardArea.getChildren().add(cardTitle);
        
        // Add a separator
        cardArea.getChildren().add(new Separator());
    }

    private void setupLayout() {
        VBox userArea = createPlayerArea("Your Name", Colour.BLUE, false);
        VBox cpu1Area = createPlayerArea("CPU 1", Colour.GREEN, true);
        VBox cpu2Area = createPlayerArea("CPU 2", Colour.YELLOW, true);
        VBox cpu3Area = createPlayerArea("CPU 3", Colour.RED, true);
        StackPane boardPane = new StackPane(board);
        boardPane.setPrefSize(800, 800);
        BorderPane mainPane = new BorderPane();
        mainPane.setCenter(boardPane);
        mainPane.setBottom(userArea);
        mainPane.setLeft(cpu1Area);
        mainPane.setTop(cpu2Area);
        mainPane.setRight(cpu3Area);
        mainPane.setPadding(new Insets(30));
        VBox userHandBox = new VBox(cardArea);
        userHandBox.setAlignment(Pos.CENTER);
        userHandBox.setPadding(new Insets(10,0,0,0));
        // Add play/cancel buttons below the cards
        HBox buttonBar = new HBox(10, playButton, cancelButton);
        buttonBar.setAlignment(Pos.CENTER);
        buttonBar.setPadding(new Insets(10,0,0,0));
        userArea.getChildren().addAll(userHandBox, buttonBar);
        root.setCenter(mainPane);
    }

    private VBox createPlayerArea(String name, Colour colour, boolean isCPU) {
        VBox area = new VBox(5);
        area.setAlignment(Pos.CENTER);
        // Avatar
        Circle avatar = new Circle(30, convertColour(colour));
        avatar.setStroke(javafx.scene.paint.Color.DARKGRAY);
        avatar.setStrokeWidth(3);
        // Label
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        // Hand (CPU: card backs, User: empty for now)
        HBox hand = new HBox(5);
        hand.setAlignment(Pos.CENTER);
        if (isCPU) {
            for (int i = 0; i < 5; i++) {
                Rectangle cardBack = new Rectangle(32, 48);
                cardBack.setArcWidth(8);
                cardBack.setArcHeight(8);
                cardBack.setFill(javafx.scene.paint.Color.SADDLEBROWN);
                cardBack.setStroke(javafx.scene.paint.Color.BEIGE);
                hand.getChildren().add(cardBack);
            }
        }
        area.getChildren().addAll(avatar, nameLabel, hand);
        return area;
    }

    public void updatePlayerInfo(String playerName, Colour colour, int turn) {
        playerLabel.setText("Player: " + playerName + " (" + colour + ")");
        turnLabel.setText("Turn: " + turn);
    }

    public void updateNextPlayer(String playerName, Colour colour) {
        nextPlayerLabel.setText("Next Player: " + playerName + " (" + colour + ")");
    }

    public void updateStatus(String status) {
        statusLabel.setText("Status: " + status);
        if (status.toLowerCase().contains("error")) {
            statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #f44336;");
        } else if (status.toLowerCase().contains("success")) {
            statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #4CAF50;");
        } else {
            statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0066CC;");
        }
    }

    public void updateCards(List<Card> cards) {
        Node title = cardArea.getChildren().get(0);
        Node separator = cardArea.getChildren().get(1);
        cardArea.getChildren().clear();
        cardArea.getChildren().add(title);
        cardArea.getChildren().add(separator);
        HBox cardRow = new HBox(10);
        cardRow.setAlignment(Pos.CENTER);
        cardRow.setPadding(new Insets(10));
        for (Card card : cards) {
            VBox cardContainer = new VBox(5);
            cardContainer.setAlignment(Pos.CENTER);
            cardContainer.setStyle("-fx-background-color: white; -fx-border-color: #000000; -fx-border-width: 2px; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2); -fx-min-width: 120px; -fx-min-height: 160px; -fx-padding: 10px; -fx-cursor: hand;");
            Label cardName = new Label(card.getName());
            cardName.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-alignment: center;");
            Label cardDesc = new Label(card.getDescription());
            cardDesc.setStyle("-fx-font-size: 11px; -fx-text-alignment: center; -fx-wrap-text: true;");
            cardDesc.setMaxWidth(100);
            cardContainer.getChildren().addAll(cardName, cardDesc);
            cardContainer.setOnMouseClicked(e -> {
                if (cardClickListener != null) cardClickListener.onCardClick(card);
            });
            cardContainer.setOnMouseEntered(e -> cardContainer.setStyle("-fx-background-color: #f0f8ff; -fx-border-color: #000000; -fx-border-width: 2px; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 3); -fx-min-width: 120px; -fx-min-height: 160px; -fx-padding: 10px; -fx-cursor: hand;"));
            cardContainer.setOnMouseExited(e -> cardContainer.setStyle("-fx-background-color: white; -fx-border-color: #000000; -fx-border-width: 2px; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2); -fx-min-width: 120px; -fx-min-height: 160px; -fx-padding: 10px; -fx-cursor: hand;"));
            cardContainer.setUserData(card);
            cardRow.getChildren().add(cardContainer);
        }
        cardArea.getChildren().add(cardRow);
    }

    private void updateCardDescription(Card card) {
        String description = card.toString() + "\n\n";
        if (card.toString().startsWith("7")) {
            description += "Split movement: Move two marbles a total of 7 steps\n\n";
            description += "• Enter split distance (1-6) for first marble\n";
            description += "• Second marble moves remaining steps\n";
            description += "• Can destroy opponent marbles";
        } else if (card.toString().startsWith("J")) {
            description += "Swap: Exchange positions with another player's marble\n\n";
            description += "• Select your marble and target marble\n";
            description += "• Cannot swap with marbles in safe zones\n";
            description += "• Cannot swap with marbles in home zones";
        } else if (card.toString().startsWith("Q")) {
            description += "Discard: Remove a random card from a random player's hand\n\n";
            description += "• Select target player\n";
            description += "• Cannot discard from your own hand\n";
            description += "• Card is removed from game";
        } else if (card.toString().startsWith("K")) {
            description += "King's move: Move 13 steps, destroying all marbles in path\n\n";
            description += "• Select your marble\n";
            description += "• Destroys all opponent marbles in path\n";
            description += "• Cannot destroy marbles in safe zones";
        } else if (card.toString().startsWith("A")) {
            description += "Ace: Field a marble or move 1 step\n\n";
            description += "• Press 'F' to field a marble\n";
            description += "• Or select a marble to move 1 step\n";
            description += "• Can destroy opponent marbles";
        }
        cardDescription.setText(description);
        cardDescription.setStyle("-fx-font-size: 12px; -fx-background-color: #FFFFFF;");
    }

    public void updateMarble(String zone, int position, Colour colour) {
        StackPane cell = cellViews.get(zone + "_" + position);
        if (cell != null) {
            cell.getChildren().clear();
            if (colour != null) {
                Circle marble = new Circle(15);
                marble.setFill(convertColour(colour));
                marble.setStroke(javafx.scene.paint.Color.BLACK);
                marble.setStrokeWidth(1);
                cell.getChildren().add(marble);
            }
        }
    }

    public void highlightCell(String zone, int position, boolean highlight) {
        StackPane cell = cellViews.get(zone + "_" + position);
        if (cell != null) {
            String baseStyle = cell.getStyle().replace(" -fx-border-color: #FF0000; -fx-border-width: 2px;", "");
            if (highlight) {
                cell.setStyle(baseStyle + " -fx-border-color: #FF0000; -fx-border-width: 2px; -fx-effect: dropshadow(gaussian, #FF0000, 10, 0.5, 0, 0);");
            } else {
                cell.setStyle(baseStyle);
            }
        }
    }

    public void highlightValidMoves(List<String> validMoves) {
        // Clear previous highlights
        for (StackPane cell : cellViews.values()) {
            String baseStyle = cell.getStyle().replace(" -fx-background-color: #FFD700;", "")
                                         .replace(" -fx-effect: dropshadow(gaussian, #FFD700, 10, 0.5, 0, 0);", "");
            cell.setStyle(baseStyle);
        }
        
        // Highlight valid moves
        for (String move : validMoves) {
            String[] parts = move.split("_");
            if (parts.length == 2) {
                StackPane cell = cellViews.get(move);
                if (cell != null) {
                    String baseStyle = cell.getStyle();
                    cell.setStyle(baseStyle + " -fx-background-color: #FFD700; -fx-effect: dropshadow(gaussian, #FFD700, 10, 0.5, 0, 0);");
                }
            }
        }
    }

    public void highlightCard(Card card, boolean highlight) {
        for (javafx.scene.Node node : cardArea.getChildren()) {
            if (node instanceof HBox) {
                HBox cardRow = (HBox) node;
                for (Node cardNode : cardRow.getChildren()) {
                    if (cardNode instanceof VBox) {
                        VBox cardContainer = (VBox) cardNode;
                        if (cardContainer.getUserData() == card) {
                            if (highlight) {
                                cardContainer.setStyle("-fx-background-color: #FFF8DC; " +
                                                    "-fx-border-color: #FFD700; " +
                                                    "-fx-border-width: 3px; " +
                                                    "-fx-border-radius: 10px; " +
                                                    "-fx-background-radius: 10px; " +
                                                    "-fx-effect: dropshadow(gaussian, rgba(255,215,0,0.4), 15, 0, 0, 3); " +
                                                    "-fx-min-width: 120px; " +
                                                    "-fx-min-height: 160px; " +
                                                    "-fx-padding: 10px; " +
                                                    "-fx-cursor: hand;");
                            } else {
                                cardContainer.setStyle("-fx-background-color: white; " +
                                                    "-fx-border-color: #000000; " +
                                                    "-fx-border-width: 2px; " +
                                                    "-fx-border-radius: 10px; " +
                                                    "-fx-background-radius: 10px; " +
                                                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2); " +
                                                    "-fx-min-width: 120px; " +
                                                    "-fx-min-height: 160px; " +
                                                    "-fx-padding: 10px; " +
                                                    "-fx-cursor: hand;");
                            }
                        }
                    }
                }
            }
        }
    }

    public void showSplitDistanceInput(boolean show) {
        splitDistanceBox.setVisible(show);
    }

    public String getSplitDistance() {
        return splitDistanceField.getText();
    }

    public void clearSplitDistance() {
        splitDistanceField.clear();
    }

    public void showComputerTurnProgress(boolean show) {
        computerTurnProgress.setVisible(show);
    }

    public void updateComputerTurnProgress(double progress) {
        computerTurnProgress.setProgress(progress);
    }

    private javafx.scene.paint.Color convertColour(Colour colour) {
        switch (colour) {
            case RED: return javafx.scene.paint.Color.RED;
            case BLUE: return javafx.scene.paint.Color.BLUE;
            case GREEN: return javafx.scene.paint.Color.GREEN;
            case YELLOW: return javafx.scene.paint.Color.YELLOW;
            default: return javafx.scene.paint.Color.BLACK;
        }
    }

    // Getters
    public Parent getRoot() { return root; }
    public Button getPlayButton() { return playButton; }
    public Button getCancelButton() { return cancelButton; }
    public VBox getCardArea() { return cardArea; }

    // Add a method to update the user area label
    public void updateUserAreaName(String name) {
        // Find the user area label and update it
        // (Assume user area is at the bottom and label is the second child)
        BorderPane mainPane = (BorderPane) root.getCenter();
        VBox userArea = (VBox) mainPane.getBottom();
        if (userArea != null && userArea.getChildren().size() > 1) {
            Node maybeLabel = userArea.getChildren().get(1);
            if (maybeLabel instanceof Label) {
                ((Label) maybeLabel).setText(name);
            }
        }
    }
}
