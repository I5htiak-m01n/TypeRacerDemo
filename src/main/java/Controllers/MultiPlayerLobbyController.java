package Controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import network.Client;

import java.io.IOException;

public class MultiPlayerLobbyController {
    @FXML private ListView<String> playerListView;
    @FXML private Label statusLabel;
    @FXML private Button startButton;
    @FXML private Label hostIndicator; // Now properly linked to FXML

    private Client client;
    private String playerName;
    private ObservableList<String> players = FXCollections.observableArrayList();
    private boolean isHost = false;

    public void initialize(Client client, String name, boolean isHost) {
        this.client = client;
        this.playerName = name;
        this.isHost = isHost;

        setupUI();
        setupNetworkHandlers();
        client.sendName(name);
    }

    private void setupUI() {
        playerListView.setItems(players);
        startButton.setDisable(!isHost);

        // Safe null check
        if (hostIndicator != null) {
            hostIndicator.setText(isHost ? "(Host)" : "(Player)");
        }
    }

    private void setupNetworkHandlers() {
        client.setOnMessageReceived(message -> {
            if (message.startsWith("PLAYERS:")) {
                Platform.runLater(() -> {
                    players.setAll(message.substring(8).split(","));
                    statusLabel.setText(players.size() + " players connected");
                });
            } else if (message.equals("START_GAME")) {
                Platform.runLater(this::startGame);
            }
        });
    }

    @FXML
    private void handleStartGame() {
        if (isHost) {
            client.sendStartGame();
        }
    }

    private void startGame() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmlFiles/MultiPlayerGame.fxml"));
            Parent root = loader.load();

            MultiPlayerGameController controller = loader.getController();
            controller.initialize(client, playerName);

            Stage stage = (Stage) playerListView.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("TypeRacer - " + playerName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}