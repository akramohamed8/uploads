package application;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import engine.Game;
import view.GameView;
import controller.GameController;
import model.Colour;
import java.io.IOException;

public class Main extends Application {
    private Game game;
    private GameView view;
    private GameController controller;

    @Override
    public void start(Stage primaryStage) {
        try {
            // Get player name using a custom dialog
            String playerName = getPlayerName(primaryStage);
            if (playerName == null || playerName.trim().isEmpty()) {
                playerName = "Player 1"; // Default name if none provided
            }

            // Initialize game components
            game = new Game(playerName);
            view = new GameView(game.getBoard());
            controller = new GameController(game, view);

            // Set up the scene
            Scene scene = new Scene(view.getRoot(), 1300, 1000);  // Increased window size
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

            // Set up the stage
            primaryStage.setTitle("Jackaroo - " + playerName);
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.show();

            // Set up keyboard shortcuts
            setupKeyboardShortcuts(scene);

            // Handle window close
            primaryStage.setOnCloseRequest(e -> {
                if (controller != null) {
                    controller.shutdown();
                }
            });

            // Start the game - initialize board, deal cards, etc.
            controller.initializeGame();

        } catch (IOException e) {
            showError("Failed to initialize game: " + e.getMessage());
            System.exit(1);
        }
    }

    private String getPlayerName(Stage primaryStage) {
        // Create a custom dialog using basic JavaFX components
        Stage dialog = new Stage();
        dialog.initOwner(primaryStage);
        dialog.setTitle("Welcome to Jackaroo");

        TextField nameField = new TextField();
        nameField.setPromptText("Enter your name");
        nameField.setMaxWidth(200);

        Button okButton = new Button("OK");
        Button cancelButton = new Button("Cancel");

        okButton.setOnAction(e -> dialog.close());
        cancelButton.setOnAction(e -> {
            nameField.setText(null);
            dialog.close();
        });

        HBox buttons = new HBox(10, okButton, cancelButton);
        buttons.setAlignment(Pos.CENTER);

        Label welcomeLabel = new Label("Welcome to Jackaroo!");
        welcomeLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        Label instructionLabel = new Label(
            "• Each player has 4 marbles\n" +
            "• Move marbles from Home Zone to Base Cell using Ace or King\n" +
            "• Race your marbles around the track to your Safe Zone\n" +
            "• First player to get all marbles to Safe Zone wins!"
        );
        instructionLabel.setStyle("-fx-font-size: 12px;");

        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(25));
        content.getChildren().addAll(
            welcomeLabel,
            instructionLabel,
            new Label("Enter Your Name:"),
            nameField,
            buttons
        );

        Scene dialogScene = new Scene(content, 350, 300);
        dialog.setScene(dialogScene);
        dialog.showAndWait();

        return nameField.getText();
    }

    private void setupKeyboardShortcuts(Scene scene) {
        // Add keyboard shortcut for fielding marbles (F key)
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case F:
                    controller.fieldMarbleShortcut();
                    break;
                case ESCAPE:
                    controller.handleCancelButton();
                    break;
                default:
                    break;
            }
        });
    }

    private void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
