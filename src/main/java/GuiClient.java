// Kevin Cox - CS 342 - Project 3
// Checkers - Client Side
// Online checkers game that allows two users to play a standard game of checkers
// Has an online chat for users to chat while playing
// Records are kept for each username

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class GuiClient extends Application{


	private TextField usernameField;
	private TextField chatField;
	private TextField moveField;


	private Button loginButton;
	private Button sendMoveButton;
	private Button sendChatButton;
	private Button playAgainButton;
	private Button quitButton;


	private Label matchLabel;
	private Label statusLabel;
	private Label resultLabel;
	private Label recordLabel;
	private Client clientConnection;

	private GridPane boardGrid;
	private ListView<String> messageList;

	private boolean loggedIn = false;
	private boolean gameOver = false;
	private String currentUsername = "";
	private String myColor = "";


	private String currentBoardText = "";
	private int selectedRow = -1;
	private int selectedCol = -1;
	private static final int CELL_SIZE = 70;


	private VBox boardBox;

	
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		messageList = new ListView<>();

		clientConnection = new Client(data -> {
			Platform.runLater(() -> {
				Message msg = (Message) data;
				handleMessage(msg);
			});
		});

		clientConnection.start();

		usernameField = new TextField();
		usernameField.setPromptText("Enter username");

		moveField = new TextField();
		moveField.setPromptText("Enter move: fromRow,fromCol,toRow,toCol");
		moveField.setDisable(true);

		chatField = new TextField();
		chatField.setPromptText("Enter chat message");
		chatField.setDisable(true);

		loginButton = new Button("Login");
		sendMoveButton = new Button("Send Move");
		sendMoveButton.setDisable(true);

		sendChatButton = new Button("Send Chat");
		sendChatButton.setDisable(true);

		playAgainButton = new Button("Play Again");
		playAgainButton.setVisible(false);
		playAgainButton.setDisable(true);

		quitButton = new Button("Quit");
		quitButton.setVisible(false);

		playAgainButton.setOnAction(e -> {
			Message playAgainMsg = new Message(currentUsername, "SERVER", "", "PLAY_AGAIN");
			clientConnection.send(playAgainMsg);
			playAgainButton.setDisable(true);
			messageList.getItems().add("Play again request sent.");
		});

		quitButton.setOnAction(e -> {
			Message quitMsg = new Message(currentUsername, "SERVER", "", "QUIT");
			clientConnection.send(quitMsg);
			Platform.exit();
			System.exit(0);
		});

		matchLabel = new Label("Not matched yet");
		statusLabel = new Label("Not logged in");
		recordLabel = new Label("Record: 0W - 0L - 0D");

		boardGrid = new GridPane();
		boardGrid.setHgap(0);
		boardGrid.setVgap(0);
		drawBoard();

		resultLabel = new Label("");
		resultLabel.setVisible(false);
		resultLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

		loginButton.setOnAction(e -> handleLogin());
		sendMoveButton.setOnAction(e -> handleMove());
		sendChatButton.setOnAction(e -> handleChat());

		HBox loginBox = new HBox(10, usernameField, loginButton);
		HBox moveBox = new HBox(10, moveField, sendMoveButton);
		HBox chatBox = new HBox(10, chatField, sendChatButton);

		VBox topBox = new VBox(10, loginBox, matchLabel, statusLabel, recordLabel);
		boardBox = new VBox(10, boardGrid);
		VBox centerBox = new VBox(10, new Label("Board"), boardBox, moveBox, chatBox);
		VBox rightBox = new VBox(10, new Label("Messages"), messageList);

		topBox.setPadding(new Insets(10));
		centerBox.setPadding(new Insets(10));
		rightBox.setPadding(new Insets(10));

		rightBox.setPrefWidth(260);

		BorderPane root = new BorderPane();
		root.setTop(topBox);
		root.setCenter(centerBox);
		root.setRight(rightBox);

		Scene scene = new Scene(root, 1050, 850);

		primaryStage.setScene(scene);
		primaryStage.setTitle("Checkers Client");
		primaryStage.show();
	}

	private void handleLogin() {
		String username = usernameField.getText().trim();

		if(username.isEmpty()) {
			messageList.getItems().add("Please enter a username.");
			return;
		}

		Message loginMsg = new Message(username, "SERVER", "", "LOGIN");
		clientConnection.send(loginMsg);
	}

	private void handleMove() {
		if(!loggedIn) {
			messageList.getItems().add("Please log in first.");
			return;
		}

		if(gameOver) {
			messageList.getItems().add("The game is over.");
			return;
		}

		String moveText = moveField.getText().trim();

		if(moveText.isEmpty()) {
			messageList.getItems().add("Enter a move first.");
			return;
		}

		Message moveMsg = new Message(currentUsername, "SERVER", moveText, "MOVE");
		clientConnection.send(moveMsg);

		moveField.clear();
		clearSelection();
	}

	private void handleChat() {
		if(!loggedIn) {
			messageList.getItems().add("Please log in first.");
			return;
		}

		String chatText = chatField.getText().trim();

		if(chatText.isEmpty()) {
			messageList.getItems().add("Enter a chat message first.");
			return;
		}

		Message chatMsg = new Message(currentUsername, "SERVER", chatText, "CHAT");
		clientConnection.send(chatMsg);

		chatField.clear();
	}

	private void handleMessage(Message msg) {
		String type = msg.getType();

		if("LOGIN".equals(type)) {
			loggedIn = true;
			currentUsername = usernameField.getText().trim();

			usernameField.setDisable(true);
			loginButton.setDisable(true);

			statusLabel.setText("Logged in as: " + currentUsername);
			messageList.getItems().add("Login successful.");
		}
		else if("MATCH".equals(type)) {
			matchLabel.setText(msg.getContent());
			messageList.getItems().add(msg.getContent());

			if(msg.getContent().toLowerCase().contains("you are red")) {
				myColor = "red";
			} else if(msg.getContent().toLowerCase().contains("you are black")) {
				myColor = "black";
			}

			gameOver = false;

			boardGrid.setVisible(true);
			resultLabel.setVisible(false);
			resultLabel.setText("");
			playAgainButton.setVisible(false);
			playAgainButton.setDisable(true);
			quitButton.setVisible(false);

			boardBox.getChildren().clear();
			boardBox.getChildren().add(boardGrid);
			boardBox.setAlignment(javafx.geometry.Pos.TOP_CENTER);

			moveField.setDisable(false);
			chatField.setDisable(false);
			sendMoveButton.setDisable(false);
			sendChatButton.setDisable(false);
			clearSelection();
		}
		else if("BOARD".equals(type)) {
			currentBoardText = msg.getContent();
			drawBoard();
		}
		else if("GAME_STATUS".equals(type)) {
			statusLabel.setText(msg.getContent());
			messageList.getItems().add(msg.getContent());
			if("RED_WINS".equals(msg.getContent()) ||
					"BLACK_WINS".equals(msg.getContent()) ||
					"DRAW".equals(msg.getContent())) {

				gameOver = true;


				if("DRAW".equals(msg.getContent())) {
					resultLabel.setText("DRAW!");
					resultLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: blue;");
				} else {

					boolean iWon =
							("RED_WINS".equals(msg.getContent()) && "red".equals(myColor)) ||
									("BLACK_WINS".equals(msg.getContent()) && "black".equals(myColor));

					if(iWon) {
						resultLabel.setText("YOU WIN!");
						resultLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: green;");
					} else {
						resultLabel.setText("YOU LOSE!");
						resultLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: red;");
					}
				}

				boardBox.setAlignment(javafx.geometry.Pos.CENTER);

				resultLabel.setVisible(true);
				playAgainButton.setVisible(true);
				playAgainButton.setDisable(false);
				quitButton.setVisible(true);

				boardBox.getChildren().clear();
				boardBox.getChildren().addAll(resultLabel, playAgainButton, quitButton);
				boardBox.setAlignment(javafx.geometry.Pos.CENTER);

				moveField.setDisable(true);
				sendMoveButton.setDisable(true);
				chatField.setDisable(true);
				sendChatButton.setDisable(true);
			}


		} else if("STATS".equals(type)){
			recordLabel.setText(msg.getContent());
			messageList.getItems().add(msg.getContent());
		} else if("CHAT".equals(type)) {
			messageList.getItems().add(msg.toString());
		} else if("MOVE".equals(type)) {
			messageList.getItems().add(msg.toString());
		} else if("ERROR".equals(type)) {
			messageList.getItems().add("ERROR: " + msg.getContent());
		}else {
			messageList.getItems().add(msg.toString());
		}
	}

	private void drawBoard() {
		boardGrid.getChildren().clear();

		int[][] boardValues = parseBoardText(currentBoardText);

		for(int i = 0; i < 8; i++) {
			for(int j = 0; j < 8; j++) {
				StackPane cell = new StackPane();

				Rectangle square = new Rectangle(CELL_SIZE, CELL_SIZE);

				if((i + j) % 2 == 0) {
					square.setFill(Color.BLACK);
				} else {
					square.setFill(Color.WHITE);
				}

				if(i == selectedRow && j == selectedCol) {
					square.setStroke(Color.YELLOW);
					square.setStrokeWidth(4);
				} else {
					square.setStroke(Color.BLACK);
					square.setStrokeWidth(1);
				}

				cell.getChildren().add(square);

				int pieceValue = boardValues[i][j];

				if(pieceValue != 0) {
					Circle piece = new Circle(CELL_SIZE / 2.8);

					if(pieceValue == 1 || pieceValue == 3) {
						piece.setFill(Color.RED);
						piece.setStroke(Color.DARKRED);
					} else if(pieceValue == 2 || pieceValue == 4) {
						piece.setFill(Color.BLACK);
						piece.setStroke(Color.GRAY);
					}

					piece.setStrokeWidth(2);
					cell.getChildren().add(piece);

					if(pieceValue == 3 || pieceValue == 4) {
						Circle kingMark = new Circle(CELL_SIZE / 7.0);
						kingMark.setFill(Color.GOLD);
						kingMark.setStroke(Color.WHITE);
						kingMark.setStrokeWidth(2);
						cell.getChildren().add(kingMark);
					}
				}

				final int r = i;
				final int c = j;

				cell.setOnMouseClicked(e -> handleBoardClick(r, c));

				boardGrid.add(cell, j, i);
			}
		}
	}

	private int[][] parseBoardText(String boardText) {
		int[][] values = new int[8][8];

		if(boardText == null || boardText.trim().isEmpty()) {
			return values;
		}

		String[] rows = boardText.trim().split("\\n");

		for(int i = 0; i < rows.length && i < 8; i++) {
			String[] cols = rows[i].trim().split("\\s+");

			for(int j = 0; j < cols.length && j < 8; j++) {
				try {
					values[i][j] = Integer.parseInt(cols[j]);
				} catch (NumberFormatException e) {
					values[i][j] = 0;
				}
			}
		}

		return values;
	}

	private void handleBoardClick(int row, int col) {
		if(!loggedIn) {
			messageList.getItems().add("Please log in first.");
			return;
		}

		if(gameOver) {
			messageList.getItems().add("The game is over.");
			return;
		}

		if(moveField.isDisabled()) {
			messageList.getItems().add("You are not in a match yet.");
			return;
		}

		if(selectedRow == -1 && selectedCol == -1) {
			selectedRow = row;
			selectedCol = col;
			statusLabel.setText("Selected piece at: " + row + "," + col);
			drawBoard();
		} else {
			String moveText = selectedRow + "," + selectedCol + "," + row + "," + col;
			moveField.setText(moveText);

			Message moveMsg = new Message(currentUsername, "SERVER", moveText, "MOVE");
			clientConnection.send(moveMsg);

			clearSelection();
		}
	}

	private void clearSelection() {
		selectedRow = -1;
		selectedCol = -1;
		drawBoard();
	}
}
