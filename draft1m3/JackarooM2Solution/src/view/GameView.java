package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import engine.board.Cell;
import model.Colour;
import model.card.Card;
import model.player.Marble;
import model.player.Player;

public class GameView {
    private BorderPane root;
    
    // Game board components
    private Pane boardPane;
    public Map<Cell, StackPane> cellViews;
    private Map<Marble, Circle> marbleViews;
    
    // Player information components
    private VBox playerInfoBox;
    private Label currentPlayerLabel;
    private Label turnInfoLabel;
    private Label nextPlayerLabel;
    private Label firePitLabel;
    
    // Card area components
    public HBox cardArea;
    public ArrayList<CardView> cardViews;
    
    // Home zones
    private Map<Colour, VBox> homeZones;
    
    // Action buttons
    private Button playButton;
    private Button cancelButton;
    private TextField splitDistanceField;
    
    // Notification area
    private Label notificationLabel;
    
    // Selected elements tracking
    private CardView selectedCardView;
    private ArrayList<Circle> selectedMarbleViews;
    
    // Constants for sizing and positioning
    private static final int CELL_SIZE = 30;
    private static final int MARBLE_RADIUS = 12;
    private static final int BOARD_SIZE = 100; // Number of cells in the main track
    private static final int SAFE_ZONE_SIZE = 4; // Number of cells per safe zone
    
    public GameView() {
        // Initialize components
        root = new BorderPane();
        boardPane = new Pane();
        cellViews = new HashMap<>();
        marbleViews = new HashMap<>();
        cardViews = new ArrayList<>();
        homeZones = new HashMap<>();
        selectedMarbleViews = new ArrayList<>();
        
        // Setup layout
        setupLayout();
    }
    
    private void setupLayout() {
        // Setup board area in the center
        boardPane.setPrefSize(800, 600);
        boardPane.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #cccccc; -fx-border-width: 2px;");
        
        // Player info section at the top
        setupPlayerInfoSection();
        
        // Card area at the bottom
        setupCardArea();
        
        // Action buttons and controls on the right
        setupControlArea();
        
        // Notification area
        setupNotificationArea();
        
        // Add components to the root layout
        root.setCenter(boardPane);
        root.setTop(playerInfoBox);
        root.setBottom(cardArea);
        root.setRight(setupControlArea());
        
        // Set padding and gaps
        BorderPane.setMargin(boardPane, new Insets(10));
        BorderPane.setMargin(playerInfoBox, new Insets(10));
        BorderPane.setMargin(cardArea, new Insets(10));
    }
    
    private void setupPlayerInfoSection() {
        playerInfoBox = new VBox(10);
        playerInfoBox.setPadding(new Insets(10));
        playerInfoBox.setAlignment(Pos.CENTER);
        
        currentPlayerLabel = new Label("Current Player: Player 1");
        currentPlayerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        turnInfoLabel = new Label("Turn: 1");
        turnInfoLabel.setFont(Font.font("Arial", 14));
        
        nextPlayerLabel = new Label("Next Player: Player 2");
        nextPlayerLabel.setFont(Font.font("Arial", 14));
        
        firePitLabel = new Label("Fire Pit: None");
        firePitLabel.setFont(Font.font("Arial", 14));
        
        playerInfoBox.getChildren().addAll(currentPlayerLabel, nextPlayerLabel, turnInfoLabel, firePitLabel);
    }
    
    private void setupCardArea() {
        cardArea = new HBox(20);
        cardArea.setPadding(new Insets(10));
        cardArea.setAlignment(Pos.CENTER);
        cardArea.setPrefHeight(150);
        cardArea.setStyle("-fx-background-color: #e8e8e8; -fx-border-color: #cccccc; -fx-border-width: 1px;");
    }
    
    private VBox setupControlArea() {
        VBox controlBox = new VBox(15);
        controlBox.setPadding(new Insets(10));
        controlBox.setAlignment(Pos.TOP_CENTER);
        controlBox.setPrefWidth(200);
        
        // Split distance control
        VBox splitControl = new VBox(5);
        Label splitLabel = new Label("Split Distance (1-6):");
        splitDistanceField = new TextField("3");
        splitDistanceField.setPrefWidth(100);
        
        // Add validation for the split distance field
        splitDistanceField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                splitDistanceField.setText(newVal.replaceAll("[^\\d]", ""));
            }
            try {
                int value = Integer.parseInt(splitDistanceField.getText());
                if (value < 1) splitDistanceField.setText("1");
                if (value > 6) splitDistanceField.setText("6");
            } catch (NumberFormatException e) {
                splitDistanceField.setText("3");
            }
        });
        
        splitControl.getChildren().addAll(splitLabel, splitDistanceField);
        
        // Action buttons
        playButton = new Button("Play Selected Card");
        playButton.setPrefWidth(180);
        playButton.setDisable(true);
        
        cancelButton = new Button("Cancel Selection");
        cancelButton.setPrefWidth(180);
        cancelButton.setDisable(true);
        
        // Add home zones (placeholders for now)
        Label homeZonesLabel = new Label("Home Zones:");
        homeZonesLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        GridPane homeZonesGrid = new GridPane();
        homeZonesGrid.setHgap(10);
        homeZonesGrid.setVgap(10);
        
        for (Colour colour : Colour.values()) {
            VBox homeZone = createHomeZone(colour);
            homeZones.put(colour, homeZone);
            
            // Position in the grid based on colour
            int row = colour.ordinal() / 2;
            int col = colour.ordinal() % 2;
            homeZonesGrid.add(homeZone, col, row);
        }
        
        controlBox.getChildren().addAll(splitControl, playButton, cancelButton, 
                                      new Label(""), homeZonesLabel, homeZonesGrid);
        
        return controlBox;
    }
    
    private VBox createHomeZone(Colour colour) {
        VBox homeBox = new VBox(5);
        Label homeLabel = new Label(colour.toString());
        homeLabel.setTextFill(getColorForEnum(colour));
        
        HBox marbleBox = new HBox(5);
        marbleBox.setAlignment(Pos.CENTER);
        
        // Placeholder circles for marbles in home
        for (int i = 0; i < 4; i++) {
            Circle marble = new Circle(10);
            marble.setFill(getColorForEnum(colour));
            marble.setStroke(Color.BLACK);
            marble.setVisible(false); // Initially hidden until we add marbles
            marbleBox.getChildren().add(marble);
        }
        
        homeBox.getChildren().addAll(homeLabel, marbleBox);
        return homeBox;
    }
    
    private void setupNotificationArea() {
        notificationLabel = new Label("");
        notificationLabel.setFont(Font.font("Arial", 14));
        notificationLabel.setPadding(new Insets(5));
        
        // Add to bottom of the control box or another suitable area
        // This will be implemented later when we finalize the layout
    }
    
    public void createBoard(ArrayList<Cell> track, ArrayList<ArrayList<Cell>> safeZones) {
        boardPane.getChildren().clear();
        cellViews.clear();
        
        // Create track cells
        createTrackCells(track);
        
        // Create safe zone cells
        for (int i = 0; i < safeZones.size(); i++) {
            createSafeZoneCells(safeZones.get(i), Colour.values()[i]);
        }
    }
    
    private void createTrackCells(ArrayList<Cell> track) {
        // Calculate the center of the board for positioning
        double centerX = boardPane.getPrefWidth() / 2;
        double centerY = boardPane.getPrefHeight() / 2;
        
        // Radius of the track
        double trackRadius = Math.min(centerX, centerY) - 100;
        
        for (int i = 0; i < track.size(); i++) {
            Cell cell = track.get(i);
            
            // Calculate position on a circular track
            double angle = 2 * Math.PI * i / track.size();
            double x = centerX + trackRadius * Math.cos(angle) - CELL_SIZE / 2;
            double y = centerY + trackRadius * Math.sin(angle) - CELL_SIZE / 2;
            
            // Create cell view
            StackPane cellView = createCellView(cell, x, y);
            boardPane.getChildren().add(cellView);
            cellViews.put(cell, cellView);
        }
    }
    
    private void createSafeZoneCells(ArrayList<Cell> safeZone, Colour colour) {
        // This is a simplified implementation - in a real game, you'd position these properly
        // Based on their colors and in relation to the main track
        
        // Find the base cell position for this color (assumed to be at index color.ordinal() * 25)
        int baseIndex = colour.ordinal() * 25;
        
        // Get the base cell position (if it exists in our cells map)
        double baseX = 0, baseY = 0;
        boolean foundBase = false;
        
        for (Map.Entry<Cell, StackPane> entry : cellViews.entrySet()) {
            if (entry.getValue().getLayoutX() == baseIndex) {
                baseX = entry.getValue().getLayoutX();
                baseY = entry.getValue().getLayoutY();
                foundBase = true;
                break;
            }
        }
        
        if (!foundBase) {
            // Fallback positions if base not found
            double centerX = boardPane.getPrefWidth() / 2;
            double centerY = boardPane.getPrefHeight() / 2;
            
            // Position safe zones in different corners based on color
            switch (colour) {
                case GREEN:
                    baseX = centerX - 150;
                    baseY = centerY - 150;
                    break;
                case RED:
                    baseX = centerX + 150;
                    baseY = centerY - 150;
                    break;
                case YELLOW:
                    baseX = centerX - 150;
                    baseY = centerY + 150;
                    break;
                case BLUE:
                    baseX = centerX + 150;
                    baseY = centerY + 150;
                    break;
            }
        }
        
        // Create safe zone cells in a row from the base
        for (int i = 0; i < safeZone.size(); i++) {
            Cell cell = safeZone.get(i);
            double x = baseX + (i * (CELL_SIZE + 5));
            double y = baseY;
            
            StackPane cellView = createCellView(cell, x, y);
            cellView.setStyle(cellView.getStyle() + "; -fx-background-color: " + getColorString(colour) + ";");
            
            boardPane.getChildren().add(cellView);
            cellViews.put(cell, cellView);
        }
    }
    
    private StackPane createCellView(Cell cell, double x, double y) {
        StackPane cellView = new StackPane();
        cellView.setLayoutX(x);
        cellView.setLayoutY(y);
        
        // Create cell background
        Rectangle background = new Rectangle(CELL_SIZE, CELL_SIZE);
        background.setFill(Color.WHITE);
        background.setStroke(Color.BLACK);
        
        // Style based on cell type
        switch (cell.getCellType()) {
            case BASE:
                background.setFill(Color.LIGHTBLUE);
                break;
            case ENTRY:
                background.setFill(Color.LIGHTGREEN);
                break;
            case SAFE:
                background.setFill(Color.LIGHTYELLOW);
                break;
            default:
                background.setFill(Color.WHITE);
        }
        
        // Add trap indicator if needed
        if (cell.isTrap()) {
            background.setFill(Color.RED);
        }
        
        cellView.getChildren().add(background);
        
        // Add marble if the cell has one
        if (cell.getMarble() != null) {
            addMarbleToCell(cell.getMarble(), cellView);
        }
        
        return cellView;
    }
    
    private void addMarbleToCell(Marble marble, StackPane cellView) {
        Circle marbleView = new Circle(MARBLE_RADIUS);
        marbleView.setFill(getColorForEnum(marble.getColour()));
        marbleView.setStroke(Color.BLACK);
        
        cellView.getChildren().add(marbleView);
        marbleViews.put(marble, marbleView);
    }
    
    public void updateBoard(ArrayList<Cell> track, ArrayList<ArrayList<Cell>> safeZones) {
        // Clear existing marbles from cells
        for (StackPane cellView : cellViews.values()) {
            if (cellView.getChildren().size() > 1) {
                cellView.getChildren().remove(1, cellView.getChildren().size());
            }
        }
        
        // Update track cells
        for (Cell cell : track) {
            if (cellViews.containsKey(cell) && cell.getMarble() != null) {
                addMarbleToCell(cell.getMarble(), cellViews.get(cell));
            }
        }
        
        // Update safe zone cells
        for (ArrayList<Cell> safeZone : safeZones) {
            for (Cell cell : safeZone) {
                if (cellViews.containsKey(cell) && cell.getMarble() != null) {
                    addMarbleToCell(cell.getMarble(), cellViews.get(cell));
                }
            }
        }
    }
    
    public void updatePlayerCards(ArrayList<Card> cards, Colour playerColour) {
        cardArea.getChildren().clear();
        cardViews.clear();
        
        for (Card card : cards) {
            CardView cardView = new CardView(card);
            cardViews.add(cardView);
            cardArea.getChildren().add(cardView);
        }
    }
    
    public void updatePlayerInfo(Player currentPlayer, int turn) {
        currentPlayerLabel.setText("Current Player: " + currentPlayer.getName());
        currentPlayerLabel.setTextFill(getColorForEnum(currentPlayer.getColour()));
        turnInfoLabel.setText("Turn: " + (turn + 1));
    }
    
    public void updateHomeZones(ArrayList<Player> players) {
        // Reset all home zones first
        for (VBox homeZone : homeZones.values()) {
            HBox marbleBox = (HBox) homeZone.getChildren().get(1);
            for (int i = 0; i < marbleBox.getChildren().size(); i++) {
                marbleBox.getChildren().get(i).setVisible(false);
            }
        }
        
        // Update with current marble counts
        for (Player player : players) {
            VBox homeZone = homeZones.get(player.getColour());
            if (homeZone != null) {
                HBox marbleBox = (HBox) homeZone.getChildren().get(1);
                int marbleCount = player.getMarbles().size();
                
                for (int i = 0; i < marbleCount && i < marbleBox.getChildren().size(); i++) {
                    marbleBox.getChildren().get(i).setVisible(true);
                }
            }
        }
    }
    
    public void showNotification(String message) {
        notificationLabel.setText(message);
        
        // For more important notifications, show an alert
        if (message.contains("Exception") || message.contains("Error")) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Game Notification");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }
    }
    
    // Selection methods
    public void selectCard(Card card) {
        // Deselect previously selected card
        if (selectedCardView != null) {
            selectedCardView.setSelected(false);
        }
        
        // Find and select the new card
        for (CardView cardView : cardViews) {
            if (cardView.getCard() == card) {
                cardView.setSelected(true);
                selectedCardView = cardView;
                break;
            }
        }
        
        // Enable/disable buttons as needed
        playButton.setDisable(selectedCardView == null);
        cancelButton.setDisable(selectedCardView == null && selectedMarbleViews.isEmpty());
    }
    
    public void selectMarble(Marble marble, boolean selected) {
        Circle marbleView = marbleViews.get(marble);
        
        if (marbleView != null) {
            if (selected) {
                // Highlight selected marble
                marbleView.setStroke(Color.YELLOW);
                marbleView.setStrokeWidth(3);
                selectedMarbleViews.add(marbleView);
            } else {
                // Deselect the marble
                marbleView.setStroke(Color.BLACK);
                marbleView.setStrokeWidth(1);
                selectedMarbleViews.remove(marbleView);
            }
        }
        
        // Enable/disable buttons as needed
        cancelButton.setDisable(selectedCardView == null && selectedMarbleViews.isEmpty());
        playButton.setDisable(selectedCardView == null);
    }
    
    public void clearSelections() {
        // Clear card selection
        if (selectedCardView != null) {
            selectedCardView.setSelected(false);
            selectedCardView = null;
        }
        
        // Clear marble selections
        for (Circle marbleView : selectedMarbleViews) {
            marbleView.setStroke(Color.BLACK);
            marbleView.setStrokeWidth(1);
        }
        selectedMarbleViews.clear();
        
        // Disable action buttons
        playButton.setDisable(true);
        cancelButton.setDisable(true);
    }
    
    // Helper methods
    private Color getColorForEnum(Colour colour) {
        switch (colour) {
            case GREEN: return Color.GREEN;
            case RED: return Color.RED;
            case YELLOW: return Color.GOLD;
            case BLUE: return Color.BLUE;
            default: return Color.BLACK;
        }
    }
    
    private String getColorString(Colour colour) {
        switch (colour) {
            case GREEN: return "#00FF00";
            case RED: return "#FF0000";
            case YELLOW: return "#FFFF00";
            case BLUE: return "#0000FF";
            default: return "#000000";
        }
    }
    
    // Getters for controller access
    public BorderPane getRoot() {
        return root;
    }
    
    public Button getPlayButton() {
        return playButton;
    }
    
    public Button getCancelButton() {
        return cancelButton;
    }
    
    public int getSplitDistance() {
        try {
            return Integer.parseInt(splitDistanceField.getText());
        } catch (NumberFormatException e) {
            return 3; // Default value
        }
    }
    
    // Inner class for card views
    public class CardView extends StackPane {
        private Card card;
        public Rectangle background;
        private Label nameLabel;
        private Label descriptionLabel;
        
        public CardView(Card card) {
            this.card = card;
            
            setPrefSize(120, 180);
            setPadding(new Insets(5));
            
            background = new Rectangle(110, 170);
            background.setFill(Color.WHITE);
            background.setStroke(Color.BLACK);
            background.setArcWidth(15);
            background.setArcHeight(15);
            
            VBox content = new VBox(5);
            content.setPadding(new Insets(10));
            content.setAlignment(Pos.TOP_CENTER);
            
            nameLabel = new Label(card.getName());
            nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            nameLabel.setWrapText(true);
            
            descriptionLabel = new Label(card.getDescription());
            descriptionLabel.setFont(Font.font("Arial", 12));
            descriptionLabel.setWrapText(true);
            
            // Add suit label if available
            String suitText = "";
            try {
                java.lang.reflect.Method getSuit = card.getClass().getMethod("getSuit");
                Object suit = getSuit.invoke(card);
                if (suit != null) {
                    suitText = "Suit: " + suit.toString();
                }
            } catch (Exception ignored) {}
            Label suitLabel = new Label(suitText);
            suitLabel.setFont(Font.font("Arial", 12));
            suitLabel.setWrapText(true);

            content.getChildren().addAll(nameLabel, suitLabel, descriptionLabel);
            
            getChildren().addAll(background, content);
            
            // Add tooltip for longer descriptions
            Tooltip tooltip = new Tooltip(card.getDescription());
            Tooltip.install(this, tooltip);
            
            // Add click handler
            setOnMouseClicked(e -> {
                setSelected(true);
            });
        }
        
        public Card getCard() {
            return card;
        }
        
        public void setSelected(boolean selected) {
            if (selected) {
                background.setStroke(Color.YELLOW);
                background.setStrokeWidth(3);
                toFront();
            } else {
                background.setStroke(Color.BLACK);
                background.setStrokeWidth(1);
            }
        }
    }
    
    public void updateNextPlayer(String name, String colour) {
        nextPlayerLabel.setText("Next Player: " + name + " (" + colour + ")");
    }
    
    public void updateFirePit(String cardName) {
        firePitLabel.setText("Fire Pit: " + cardName);
    }
}