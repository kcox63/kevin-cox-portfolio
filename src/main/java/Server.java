import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.HashMap;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;

public class Server{

	int count = 1;	
	ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
	HashMap<String, ClientThread> userMap = new HashMap<String, ClientThread>();
	HashMap<String, WinLossRecord> records = new HashMap<String, WinLossRecord>();
	private static final String RECORDS_FILE = "playerRecords.dat";
	ClientThread waitingPlayer = null;

	TheServer server;
	private Consumer<Serializable> callback;
	
	
	Server(Consumer<Serializable> call){
		callback = call;
		loadRecords();
		server = new TheServer();
		server.start();
	}

	@SuppressWarnings("unchecked")
	private void loadRecords() {
		try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(RECORDS_FILE))) {
			records = (HashMap<String, WinLossRecord>) in.readObject();
		} catch(FileNotFoundException e) {
			records = new HashMap<String, WinLossRecord>();
		} catch(Exception e) {
			records = new HashMap<String, WinLossRecord>();
			callback.accept("Could not load records. Starting fresh.");
		}
	}

	private void saveRecords() {
		try(ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(RECORDS_FILE))) {
			out.writeObject(records);
		} catch (Exception e) {
			callback.accept("Could not save records.");
		}
	}

	private WinLossRecord getOrCreateRecord(String username) {
		WinLossRecord record = records.get(username);
		if(record == null) {
			record = new WinLossRecord();
			records.put(username, record);
		}
		return record;
	}

	private void sendStatsToClient(ClientThread client) {
		if(client != null && client.username != null) {
			WinLossRecord record = getOrCreateRecord(client.username);
			client.sendMessage(new Message("SERVER", client.username, record.toString(), "STATS"));
		}
	}

	private void recordFinishedGame(GamePlay game, String status, ClientThread clientA, ClientThread clientB) {
		if(game == null || game.isResultRecorded()) {
			return;
		}

		game.setResultRecorded(true);

		String redUser = game.getP1().getUsername();
		String blackUser = game.getP2().getUsername();

		WinLossRecord redRecord = getOrCreateRecord(redUser);
		WinLossRecord blackRecord = getOrCreateRecord(blackUser);

		if("RED_WINS".equals(status)) {
			redRecord.addWin();
			blackRecord.addLoss();
		}else if("BLACK_WINS".equals(status)) {
			redRecord.addLoss();
			blackRecord.addWin();
		}else if("DRAW".equals(status)) {
			redRecord.addDraw();
			blackRecord.addDraw();
		}else {
			return;
		}

		saveRecords();
		sendStatsToClient(clientA);
		sendStatsToClient(clientB);
	}
	
	
	public class TheServer extends Thread{
		
		public void run() {

			try (ServerSocket mysocket = new ServerSocket(5555);) {
				System.out.println("Server is waiting for a client!");


				while(true) {
					ClientThread c = new ClientThread(mysocket.accept(), count);
					callback.accept("client has connected to server: " + "client #" + count);
					clients.add(c);
					c.start();
					count++;
				}
			}//end of try
			catch(Exception e) {
				callback.accept("Server socket did not launch");
				e.printStackTrace();
			}
		}
	}//end of while

	

	class ClientThread extends Thread{

		Socket connection;
		int count;
		ObjectInputStream in;
		ObjectOutputStream out;
		String username;
		GamePlay currentGame;
		ClientThread opponent;
		Player player;
		boolean wantsReplay;

		ClientThread(Socket s, int count){
			this.connection = s;
			this.count = count;
			this.username = null;
			this.currentGame = null;
			this.opponent = null;
			this.player = null;
			this.wantsReplay = false;
			}

		public void sendMessage(Message message){
			try{
				out.writeObject(message);
				out.flush();
			} catch(Exception e){
				callback.accept("Could not send message to client #" + count);
				}
		}


		public void matchPlayers(){
			if(waitingPlayer == null){
				waitingPlayer = this;
				sendMessage(new Message("SERVER", username, "Waiting for an opponent...", "MATCH"));
				callback.accept(username + " is waiting for a match");
			} else{
				ClientThread otherPlayer = waitingPlayer;
				waitingPlayer = null;

				Player p1 = new Player(otherPlayer.username, "red");
				Player p2 = new Player(this.username, "black");

				GamePlay game = new GamePlay(p1, p2);
				otherPlayer.currentGame = game;
				this.currentGame = game;

				otherPlayer.player = p1;
				this.player = p2;

				otherPlayer.opponent = this;
				this.opponent = otherPlayer;

				otherPlayer.sendMessage(new Message("SERVER", otherPlayer.username, "Match started. You are red. Opponent: " + this.username, "MATCH"));

				this.sendMessage(new Message("SERVER", this.username, "Match started. You are black. Opponent: " + otherPlayer.username, "MATCH"));

				String boardState = game.getBoard().boardString();

				otherPlayer.sendMessage(new Message("SERVER", otherPlayer.username, boardState, "BOARD"));
				this.sendMessage(new Message("SERVER", this.username, boardState, "BOARD"));

				String nextTurn;
				if(currentGame.isForcedJumpActive()){
					nextTurn = "Current turn: " + currentGame.getCurrentTurn().getUsername() + " (must continue jump from " + currentGame.getForcedJumpRow() + "," + currentGame.getForcedJumpCol() + ")";
				} else {
					nextTurn = "Current turn: " + currentGame.getCurrentTurn().getUsername();
				}
				otherPlayer.sendMessage(new Message("SERVER", otherPlayer.username, nextTurn, "GAME_STATUS"));
				this.sendMessage(new Message("SERVER", this.username, nextTurn, "GAME_STATUS"));

				callback.accept("Match created: " + otherPlayer.username + " vs " + this.username);
			}
		}
			
		public void run(){
					
			try {
				out = new ObjectOutputStream(connection.getOutputStream());
				in = new ObjectInputStream(connection.getInputStream());
				connection.setTcpNoDelay(true);
			}
			catch(Exception e) {
				callback.accept("Streams not open for client #" + count);
				return;
			}


			while(true) {
				try {
					Message message = (Message) in.readObject();
					if(message.getType().equals("LOGIN")){
						String requestedName = message.getSender();
						if(requestedName == null || requestedName.trim().isEmpty()){
							Message errorMsg = new Message("SERVER", (String) null, "Username cannot be empty", "ERROR");
							sendMessage(errorMsg);
						} else if(userMap.containsKey(requestedName)){
							Message errorMsg = new Message("SERVER", requestedName, "Username already taken", "ERROR");
							sendMessage(errorMsg);
						} else{
							username = requestedName;
							userMap.put(username, this);

							sendMessage(new Message("SERVER", username, "Login successful", "LOGIN"));
							callback.accept(username + " joined the server");
							getOrCreateRecord(username);
							saveRecords();
							sendStatsToClient(this);
							matchPlayers();
						}
					} else if(message.getType().equals("CHAT")){
						if (currentGame == null || opponent == null) {
							sendMessage(new Message("SERVER", username, "You are not in a game yet.", "ERROR"));
						} else {
							callback.accept(message.toString());
							opponent.sendMessage(message);
							sendMessage(message);
						}
					}else if(message.getType().equals("MOVE")){
						if (currentGame == null) {
							sendMessage(new Message("SERVER", username, "You are not in a game.", "ERROR"));
							continue;
						}

						String currentStatus = currentGame.gameStatus();
						if(!currentStatus.equals("ONGOING")) {
							sendMessage(new Message("SERVER", username, currentStatus, "GAME_STATUS"));
							if (opponent != null) {
								opponent.sendMessage(new Message("SERVER", opponent.username, currentStatus, "GAME_STATUS"));
							}
							recordFinishedGame(currentGame, currentStatus, this, opponent);
							continue;
						}

						try {
							String[] parts = message.getContent().split(",");

							if(parts.length != 4) {
								sendMessage(new Message("SERVER", username, "Bad move format.", "ERROR"));
								continue;
							}

							int fRow = Integer.parseInt(parts[0].trim());
							int fCol = Integer.parseInt(parts[1].trim());
							int tRow = Integer.parseInt(parts[2].trim());
							int tCol = Integer.parseInt(parts[3].trim());

							boolean moved = currentGame.makeMove(username, fRow, fCol, tRow, tCol);

							if(moved) {
								String boardState = currentGame.getBoard().boardString();

								sendMessage(new Message("SERVER", username, boardState, "BOARD"));

								if(opponent != null) {
									opponent.sendMessage(new Message("SERVER", opponent.username, boardState, "BOARD"));
								}

								String nextTurn = "Current turn: " + currentGame.getCurrentTurn().getUsername();
								sendMessage(new Message("SERVER", username, nextTurn, "GAME_STATUS"));

								if(opponent != null) {
									opponent.sendMessage(new Message("SERVER", opponent.username, nextTurn, "GAME_STATUS"));
								}

								String status = currentGame.gameStatus();
								if(!status.equals("ONGOING")) {
									sendMessage(new Message("SERVER", username, status, "GAME_STATUS"));
									if(opponent != null) {
										opponent.sendMessage(new Message("SERVER", opponent.username, status, "GAME_STATUS"));
									}
									recordFinishedGame(currentGame, status, this, opponent);
								}

								callback.accept(username + " moved: " + message.getContent());
							} else {
								sendMessage(new Message("SERVER", username, "Invalid move.", "ERROR"));
							}

						} catch(Exception e) {
							sendMessage(new Message("SERVER", username, "Bad move format.", "ERROR"));
						}
					}else if(message.getType().equals("PLAY_AGAIN")) {

						callback.accept(username + " wants to play again.");

						ClientThread oldOpponent = opponent;

						if(oldOpponent != null) {
							oldOpponent.opponent = null;
							oldOpponent.currentGame = null;
							oldOpponent.player = null;
							oldOpponent.wantsReplay = false;

							oldOpponent.sendMessage(new Message("SERVER", oldOpponent.username,
									username + " left for a new match.", "GAME_STATUS"));
						}

						this.opponent = null;
						this.currentGame = null;
						this.player = null;
						this.wantsReplay = false;

						if(waitingPlayer == this) {
							waitingPlayer = null;
						}

						matchPlayers();
					}else if (message.getType().equals("QUIT")) {
						callback.accept(username + " quit the game.");

						if(opponent != null) {
							opponent.sendMessage(new Message("SERVER", opponent.username, "Opponent left the game. You win by default.", "GAME_STATUS"));

							opponent.opponent = null;
							opponent.currentGame = null;
							opponent.player = null;
							opponent.wantsReplay = false;
						}

						if(waitingPlayer == this) {
							waitingPlayer = null;
						}


						if(username != null) {
							userMap.remove(username);
						}

						clients.remove(this);

						try {
							connection.close();
						} catch(Exception e) {}

						break;
					} else {
						sendMessage(new Message("SERVER", username, "Unknown message type", "ERROR"));
					}

				} catch(Exception e) {
					callback.accept("Something went wrong with client #" + count + ". Closing down.");

					if (waitingPlayer == this) {
						waitingPlayer = null;
					}

					if (opponent != null) {
						opponent.sendMessage(new Message("SERVER", opponent.username, username + " disconnected.", "ERROR"));
						opponent.opponent = null;
						opponent.currentGame = null;
						opponent.player = null;
					}

					if(username != null) {
						userMap.remove(username);
					}

					clients.remove(this);

					try {
						connection.close();
					} catch(Exception ex) {
					}

					break;
				}

			}
		}//end of run

			
	}//end of client thread
}


	
	

	
