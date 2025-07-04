package Controllers;

import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.application.Platform;
import javafx.animation.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.*;
import javafx.util.Duration;

import java.io.*;
import java.util.*;

public class GameController {

    @FXML
    private AnchorPane rootPane;

    @FXML
    private Label titleLabel;

    @FXML
    private TextField playerNameField;

    @FXML
    private TextFlow paragraphFlow;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Button startButton;

    @FXML
    private ListView<String> leaderboardList;

    @FXML
    private Label timeLabel;

    private List<String> inputStrings = new ArrayList<>();
    private ObservableList<String> leaderboard = FXCollections.observableArrayList();
    private String name;
    private long startTime;
    private Timeline timer;
    private boolean typingDone = false;
    private String paragraphText; //User's input
    private List<Text> textNodes = new ArrayList<>();
    private int currentIndex;
    private int correctCount;
    private int totalTyped;

    private double calculateAccuracy() {
        return totalTyped == 0 ? 0.0 : (correctCount * 100.0 / totalTyped);
    }

    private void displayParagraph(String text) {
        paragraphFlow.getChildren().clear();
        textNodes.clear();

        for (char c : text.toCharArray()) {
            Text t = new Text(String.valueOf(c));
            t.setStyle("-fx-fill: gray; -fx-font-size: 16px;");
            textNodes.add(t);
        }

        paragraphFlow.getChildren().addAll(textNodes);
    }

    private void handleCtrlBackspace() {
        if (currentIndex == 0) return;

        // Go backward from current index to find start of word
        int wordEnd = currentIndex;
        int wordStart = wordEnd - 1;

        while (wordStart >= 0 && paragraphText.charAt(wordStart) != ' ') {
            wordStart--;
        }
        wordStart++; // move to start of actual word

        for (int i = wordStart; i < wordEnd; i++) {
            Text t = textNodes.get(i);
            t.setStyle("-fx-fill: gray; -fx-font-size: 16px;");
            totalTyped = Math.max(0, totalTyped - 1);

            if (t.getText().charAt(0) == paragraphText.charAt(i)) {
                correctCount = Math.max(0, correctCount - 1);
            }
        }

        currentIndex = wordStart;
    }

    private void typingFinished() {
        if(typingDone) return;
        typingDone = true;
        if (timer != null) timer.stop();
        playerNameField.setEditable(true);
        titleLabel.setText("Type Racer");
        startButton.setText("Restart");
        timer.stop();

        long finishTime = System.currentTimeMillis() - startTime;
        double timeInSeconds = finishTime / 1000.0;
        String entry = String.format("%s - %.2f s - Accuracy: %.2f%%", name, timeInSeconds, calculateAccuracy());
        leaderboard.add(entry);

        leaderboard.sort((a,b) -> {
            double t1 = Double.parseDouble(a.split(" - ")[1].replace(" s", ""));
            double t2 = Double.parseDouble(b.split(" - ")[1].replace(" s", ""));
            return Double.compare(t1, t2);
        });
        leaderboardList.setItems(leaderboard);
    }

    private void showAlert() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("TypeRacer");
            alert.setHeaderText(null);
            alert.setContentText("Please enter your name before starting the race.");
            alert.showAndWait();
        });
    }

    @FXML
    public void initialize() {
        try{
            File file = new File("src/main/resources/txtFiles/input.txt");
            Scanner takeIn = new Scanner(file);

            while(takeIn.hasNextLine()){
                inputStrings.add(takeIn.nextLine());
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }

        progressBar.setProgress(0.0);

        playerNameField.textProperty().addListener((observable, oldValue, newValue) -> {
            if(!newValue.equals(oldValue)) {
                startButton.setText("Start");
            }
        });
        playerNameField.setOnAction(e -> {
            if(paragraphText == null || paragraphText.isEmpty()) {
                if(!playerNameField.getText().isEmpty()) {
                    onStartButtonClick();
                    playerNameField.setEditable(false);
                    Platform.runLater(() -> rootPane.requestFocus());
                }
                else showAlert();
            }
        });
        rootPane.setOnKeyPressed(event -> {
            if (event.getCode().toString().equals("BACK_SPACE") && event.isControlDown()) {
                System.out.println("Ctrl + Backspace detected!");

                // Optional: Clear last word logic here
                handleCtrlBackspace();
                event.consume(); // stop it from bubbling
            }
        });
    }

    @FXML
    public void onStartButtonClick() {
        typingDone = false;
        name = playerNameField.getText();
        if (name.isEmpty()) {
            showAlert();
            return;
        } else playerNameField.setEditable(false);
        paragraphText = inputStrings.get(new Random().nextInt(inputStrings.size()));
        displayParagraph(paragraphText);
        currentIndex = 0;
        correctCount = 0;
        totalTyped = 0;

        progressBar.setProgress(0.0);
        titleLabel.setText("Get Typing!");

        startTime = System.currentTimeMillis();
        timeLabel.setText("Time: 0.00s");

        if(timer!= null) timer.stop();
        timer = new Timeline(
                new KeyFrame(Duration.millis(100), e -> {
                    long elapsed = System.currentTimeMillis() - startTime;
                    double seconds = (double) elapsed / 1000.0;
                    timeLabel.setText(String.format("Time: %.2f", seconds));
                })
        );
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();

        startButton.setFocusTraversable(false);
        rootPane.requestFocus();
    }

    @FXML
    public void onKeyTyped(KeyEvent event) {
        if (playerNameField.isFocused()) return;
        if (paragraphText == null || paragraphText.isEmpty()) return;

        String character = event.getCharacter();
        if(character.isEmpty()) return;

        char typedChar = character.charAt(0);

        if((typedChar == '\r' || typedChar == '\n') && currentIndex > 0) {
            typingFinished();
            return;
        }

        if (typedChar == '\b') {
            if (currentIndex > 0) {
                textNodes.get(--currentIndex).setUnderline(false); //Cursor-esque shit
                Text previous = textNodes.get(currentIndex);

                if (previous.getStyle().contains("black")) {
                    correctCount--;
                }
                previous.setStyle("-fx-fill: gray; -fx-font-size: 16px;");
                totalTyped--;
            }
            return;
        }

        if (typedChar <= 32 && typedChar != ' ') return;

        if (currentIndex >= paragraphText.length()){
            typingFinished();
            return;
        }

        char expectedChar = paragraphText.charAt(currentIndex);
        Text current = textNodes.get(currentIndex);
        textNodes.get(currentIndex).setUnderline(true); // Cursor-esque shit

        if (typedChar == expectedChar) {
            current.setStyle("-fx-fill: black; -fx-font-size: 16px;");
            correctCount++;
        } else {
            current.setStyle("-fx-fill: red; -fx-font-size: 16px;");
        }

        currentIndex++;
        totalTyped++;

        progressBar.setProgress((double) currentIndex / paragraphText.length());

        if (currentIndex >= paragraphText.length()){
            typingFinished();
        }
        // Optionally move cursor effect here
    }
}
