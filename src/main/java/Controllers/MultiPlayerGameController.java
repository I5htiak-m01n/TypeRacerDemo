package Controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;

import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.*;
import javafx.util.Duration;
import network.Client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiPlayerGameController {
    @FXML private TextFlow paragraphFlow;
    @FXML private Label timeLabel;
    @FXML private Label playerNameLabel;
    //@FXML private ProgressBar progressBar;
    //@FXML private ListView<String> leaderboard;
    @FXML private Label wpmLabel;
    @FXML private Label accuracyLabel;
    @FXML private TextField typingField;
    @FXML private ListView<String> leaderboardList;

    private Client client;
    private String playerName;
    private String paragraphText;
    private long startTime;
    private Timeline timer;
    private ObservableList<String> leaderboard = FXCollections.observableArrayList();
    private int totalTyped;
    private int currentIndex;
    private int correctCharCount;
    private int correctWordCount;
    private boolean currentWordCorrect;
    private final List<Text> textNodes = new ArrayList<>();
    private boolean typingDone = false;

    // Add these fields
    private final Map<String, ProgressBar> playerProgressBars = new HashMap<>();
    @FXML private VBox progressBarsContainer;



    public void initialize(Client client, String name) {
        this.client = client;
        this.playerName = name;
        playerNameLabel.setText(playerName);
        this.leaderboardList.setItems(leaderboard);
        // Initialize with empty progress bar for current player
        addPlayerProgress(playerName);

        setupUI();
        setupEventHandlers();
        setupNetworkHandlers();
    }

    private void setupNetworkHandlers() {
        client.setOnMessageReceived(message -> {
            if (message.startsWith("PARAGRAPH:")) {
                paragraphText = message.substring(10);
//                client.sendDebugMessage("WE GOT PARA::: " + paragraph);
                Platform.runLater(this::setupParagraph);
            } else if (message.startsWith("LEADERBOARD:")) {
                Platform.runLater(() -> updateLeaderboard(message.substring(12)));
            } else if (message.startsWith("PROGRESS:")) {
                Platform.runLater(() -> updateAllProgress(message.substring(9)));
            } else if (message.startsWith("PLAYERS:")) {
                Platform.runLater(() -> {
                    String[] players = message.substring(8).split(",");
                    for (String player : players) {
                        if (!player.isEmpty() && !player.equals(playerName)) {
                            addPlayerProgress(player);
                        }
                    }
                });
            }
        });
    }

//    private void requestParagraph() {
//        client.sendMessage("SEND_PARA");
//        client.setOnMessageReceived(message -> {
//            if (message.startsWith("PARAGRAPH:")) {
//                paragraphText = message.substring(10);
//                client.sendDebugMessage("WE GOT PARA::: " + paragraph);
//                Platform.runLater(this::setupParagraph);
//            }
//        });
//    }

    private void setupUI() {
        //progressBar.setProgress(0);
        typingField.setDisable(true);
        leaderboardList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-text-fill: #d1d0c5; -fx-font-family: 'Roboto Mono';");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #d1d0c5; -fx-font-family: 'Roboto Mono';");
                }
            }
        });
        //progressBar.setStyle("-fx-accent: #e2b714; -fx-background-color: #3a3d42; -fx-border-color: #e2b714; -fx-border-width: 2;");
    }

    private void setupParagraph() {
        paragraphFlow.getChildren().clear();
        textNodes.clear();

        for (char c : paragraphText.toCharArray()) {
            Text t = new Text(String.valueOf(c));
            t.setStyle("-fx-fill: #646669;");
            textNodes.add(t);
        }
        paragraphFlow.getChildren().addAll(textNodes);
        startTimer();
        typingField.setDisable(false);
        typingField.requestFocus();
    }

    private void setupEventHandlers() {
        // Typing field listener for character-by-character comparison
        typingField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (paragraphText == null || paragraphText.isEmpty() || typingDone) return;

            // Handle backspace
            if (newValue.length() < oldValue.length()) {
                handleBackspace(oldValue, newValue);
                return;
            }
            // Handle new characters
            handleNewCharacters(oldValue, newValue);
        });

        TODO:
        // On pressing space or end of game

        // Enter key to finish
        typingField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                typingFinished();
            }
        });

        // Start typing immediately when typing field gets focus
        typingField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && paragraphText != null && !typingDone) {
                typingField.requestFocus();
            }
        });

    }

    private void startTimer() {
        startTime = System.currentTimeMillis();
        if (timer != null) timer.stop();
        timer = new Timeline(new KeyFrame(Duration.millis(100), e -> updateStats()));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    @FXML
    private void handleNewCharacters(String oldValue, String newValue) {
        for (int i = oldValue.length(); i < newValue.length(); i++) {
            if (currentIndex >= paragraphText.length()) {
                typingFinished();
                return;
            }

            char typedChar = newValue.charAt(i);
            char expectedChar = paragraphText.charAt(currentIndex);
            Text current = textNodes.get(currentIndex);
            textNodes.get(currentIndex).setUnderline(true);

            if (typedChar == expectedChar) {
                current.setStyle("-fx-fill: #d1d0c5;"); // MonkeyType's correct color
                correctCharCount++;
            } else {
                current.setStyle("-fx-fill: #ca4754;"); // MonkeyType's incorrect color
                currentWordCorrect = false;
            }

            if (typedChar == ' ') {
                if (currentWordCorrect) correctWordCount++;
                currentWordCorrect = true;
//                typingField.replaceSelection("");
            }
            currentIndex++;
            totalTyped++;
//            progressBar.setProgress((double) currentIndex / paragraphText.length());
            double progress = (double) currentIndex / paragraphText.length();
            //progressBar.setProgress(progress);
            client.sendProgress(progress);// Send progress update to server

            updateStats();

            if (currentIndex >= paragraphText.length()) {
                typingFinished();
            }
        }
    }

    private void handleBackspace(String oldValue, String newValue) {
        int diff = oldValue.length() - newValue.length();
        for (int i = 0; i < diff; i++) {
            if (currentIndex > 0) {
                currentIndex--;
                double progress = (double) currentIndex / paragraphText.length();
                //progressBar.setProgress(progress);
                client.sendProgress(progress); // Send progress update to server
                updateStats();
                Text previous = textNodes.get(currentIndex);
                previous.setStyle("-fx-fill: #646669;"); // MonkeyType's untyped color
                previous.setUnderline(false); // underline
                totalTyped = Math.max(0, totalTyped - 1);

                if (paragraphText.charAt(currentIndex) == previous.getText().charAt(0)) {
                    correctCharCount--;
                }
            }
        }
        //updateStats();
    }

    private void updateStats() {
        Platform.runLater(() -> {
            long elapsed = System.currentTimeMillis() - startTime;
            double seconds = elapsed / 1000.0;
            timeLabel.setText(String.format("%.2fs", seconds));
            wpmLabel.setText(String.format("%.2f", calculateWPM()));
            accuracyLabel.setText(String.format("%.0f%%", calculateAccuracy()));

            double time = (System.currentTimeMillis() - startTime) / 1000.0;
            double wpm = calculateWPM();
            client.sendResult(String.format("%s;%.2f;%.2f;%.2f", playerName, time, wpm, calculateAccuracy()));
        });
    }

    private double calculateAccuracy() {
        return totalTyped == 0 ? 0.0 : Math.max(0, (correctCharCount * 100.0 / totalTyped));
    }

    private double calculateWPM() {
        long elapsedMillis = System.currentTimeMillis() - startTime;
        double elapsedMinutes = elapsedMillis / 60000.0;
        return elapsedMinutes == 0 ? 0 : (correctWordCount / elapsedMinutes);
    }

    private void typingFinished() {
        if (timer != null) timer.stop();
        typingField.setDisable(true);
        double time = (System.currentTimeMillis() - startTime) / 1000.0;
        double wpm = calculateWPM();
        client.sendResult(String.format("%s;%.2f;%.2f", playerName, time, wpm));
    }

    private void updateLeaderboard(String data) {
        String[] entries = data.split("\\|");
        leaderboard.setAll(entries);
//        leaderboard.sort((a,b) -> {
//            double t1 = Double.parseDouble(a.split(" - ")[1].replace("s", ""));
//            double t2 = Double.parseDouble(b.split(" - ")[1].replace("s", ""));
//            int toReturn = Double.compare(t1, t2);
//            if (toReturn == 0) {
//                double t3 = Double.parseDouble(a.split(" - ")[2].replace(" WPM", ""));
//                double t4 = Double.parseDouble(b.split(" - ")[2].replace(" WPM", ""));
//                int toReturn2 = Double.compare(t3, t4);
//                if (toReturn2 == 0) {
//                    return a.split(" - ")[0].compareTo(b.split(" - ")[0]);
//                }
//                return toReturn2;
//            }
//            return toReturn;
//        });
        //leaderboardList.getItems().clear();
        leaderboardList.setItems(leaderboard);
    }

    // Add these new methods
    private void addPlayerProgress(String playerName) {
        if (!playerProgressBars.containsKey(playerName)) {
            ProgressBar pb = new ProgressBar(0);
            pb.setPrefWidth(1680);
            pb.setStyle("-fx-accent: " + getColorForPlayer(playerName) + ";");

            HBox playerBox = new HBox(5);
            Label nameLabel = new Label(playerName);
            nameLabel.setStyle("-fx-text-fill: #d1d0c5; -fx-font-family: 'Roboto Mono'; -fx-min-width: 75;");

            playerBox.getChildren().addAll(nameLabel, pb);
            progressBarsContainer.getChildren().add(playerBox);
            playerProgressBars.put(playerName, pb);
        }
    }

    private String getColorForPlayer(String playerName) {
        // Simple hash-based color assignment
        int hash = playerName.hashCode();
        String[] colors = {"#e2b714", "#d1d0c5", "#ca4754", "#7e57c2", "#26a69a"};
        return colors[Math.abs(hash) % colors.length];
    }

    private void updateAllProgress(String progressData) {
        String[] entries = progressData.split("\\|");
        for (String entry : entries) {
            if (!entry.isEmpty()) {
                String[] parts = entry.split(";");
                if (parts.length >= 2) {
                    String player = parts[0];
                    double progress = Double.parseDouble(parts[1]);

                    Platform.runLater(() -> {
                        if (!playerProgressBars.containsKey(player)) {
                            addPlayerProgress(player);
                        }
                        ProgressBar pb = playerProgressBars.get(player);
                        if (pb != null) {
                            pb.setProgress(progress);
                            // Highlight current player's bar
                            if (player.equals(playerName)) {
                                pb.setStyle("-fx-accent: " + getColorForPlayer(player) + "; -fx-border-color: " + getColorForPlayer(player) + ";");
                            } else {
                                pb.setStyle("-fx-accent: " + getColorForPlayer(player) + ";");
                            }
                        }
                    });
                }
            }
        }
    }
}