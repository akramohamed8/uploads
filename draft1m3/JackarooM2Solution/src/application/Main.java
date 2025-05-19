package application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;
import view.GameView;
import engine.Game;
import controller.GameController;

public class Main extends Application {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    private static final String GAME_TITLE = "Jackaroo Game";
    private static final String DEFAULT_PLAYER_NAME = "Player 1";

    @Override
    public void start(Stage primaryStage) {
        try {
            String playerName = getPlayerName(primaryStage);
            if (playerName == null) {
                showError("Game Cancelled", "No player name provided. Exiting game.");
                Platform.exit();
                return;
            }

            initializeGame(primaryStage, playerName);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error starting game", e);
            showError("Error", "Failed to start game: " + e.getMessage());
        }
    }

    private String getPlayerName(Stage primaryStage) {
        Stage dialogStage = new Stage();
        dialogStage.setTitle("Enter Name");
        
        TextField nameField = new TextField(DEFAULT_PLAYER_NAME);
        Button okButton = new Button("OK");
        Button cancelButton = new Button("Cancel");
        
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(
            new Label("Welcome to Jackaroo!"),
            new Label("Please enter your name:"),
            nameField,
            new HBox(10, okButton, cancelButton)
        );
        
        final String[] result = new String[1];
        
        okButton.setOnAction(e -> {
            result[0] = nameField.getText().trim().isEmpty() ? DEFAULT_PLAYER_NAME : nameField.getText().trim();
            dialogStage.close();
        });
        
        cancelButton.setOnAction(e -> {
            result[0] = null;
            dialogStage.close();
        });
        
        Scene dialogScene = new Scene(root);
        dialogStage.setScene(dialogScene);
        dialogStage.showAndWait();
        
        return result[0];
    }

    private void initializeGame(Stage primaryStage, String playerName) {
        try {
            Game game = new Game(playerName);
            GameView gameView = new GameView();
            GameController controller = new GameController(game, gameView);

            Scene scene = new Scene(gameView.getRoot(), WINDOW_WIDTH, WINDOW_HEIGHT);
            setupStage(primaryStage, scene);
            setupKeyboardShortcuts(scene, controller);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize game", e);
            showError("Initialization Error", "Failed to initialize game: " + e.getMessage());
            Platform.exit();
        }
    }

    private void setupStage(Stage primaryStage, Scene scene) {
        primaryStage.setTitle(GAME_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void setupKeyboardShortcuts(Scene scene, GameController controller) {
        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case F:
                    controller.fieldMarbleShortcut();
                    break;
                // Add more keyboard shortcuts here as needed
            }
        });
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
