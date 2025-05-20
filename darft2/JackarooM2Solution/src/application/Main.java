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
            if (playerName == null) {
                System.exit(0);
                return;
            }

            // Initialize game components
            game = new Game(playerName);
            view = new GameView(game.getBoard());
            controller = new GameController(game, view);

            // Set up the scene
            Scene scene = new Scene(view.getRoot(), 1300, 1000);  // Increased window size
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

            // Set up the stage
            primaryStage.setTitle("Jackaroo");
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

        VBox content = new VBox(10);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20));
        content.getChildren().addAll(
            new Label("Enter Your Name:"),
            nameField,
            buttons
        );

        Scene dialogScene = new Scene(content);
        dialog.setScene(dialogScene);
        dialog.showAndWait();

        return nameField.getText();
    }

    private void setupKeyboardShortcuts(Scene scene) {
        // Add keyboard shortcut for fielding marbles (F key)
        scene.setOnKeyPressed(e -> {
            if (e.getCode().toString().equals("F")) {
                controller.fieldMarbleShortcut();
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
