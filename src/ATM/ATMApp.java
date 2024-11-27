package ATM;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.sql.*;
import java.text.DecimalFormat;

public class ATMApp extends Application {
	private Connection connection;
	private TextArea outputArea;
	private String currentAccountId;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		connectToDatabase();

		primaryStage.setTitle("ATM System");
		BorderPane root = new BorderPane();
		root.setPadding(new Insets(20));

		CardLayoutPane cardLayoutPane = new CardLayoutPane();
		cardLayoutPane.showWelcomeScreen(root, primaryStage);

		Scene scene = new Scene(root, 700, 700);
		scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	private void connectToDatabase() {
		try {
			connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/ATM_DB", "root", "123456");
			System.out.println("Connected to database.");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	class CardLayoutPane {
		private TextField idField, pinField;

		public void showWelcomeScreen(BorderPane root, Stage primaryStage) {
			VBox welcomePanel = new VBox(10);
			welcomePanel.setAlignment(Pos.CENTER);

			Text welcomeText = new Text("Chào mừng đến với hệ thống ATM");
			welcomeText.getStyleClass().add("welcome-title");

			welcomePanel.getChildren().add(welcomeText);
			root.setCenter(welcomePanel);

			PauseTransition pause = new PauseTransition(Duration.seconds(3));
			pause.setOnFinished(event -> showLoginPanel(root, primaryStage));
			pause.play();
		}

		public void showLoginPanel(BorderPane root, Stage primaryStage) {
			VBox loginPanel = new VBox(10);
			loginPanel.setAlignment(Pos.CENTER);

			Text title = new Text("Vui lòng nhập ID và Mã PIN");
			title.getStyleClass().add("title");

			idField = new TextField();
			idField.setPromptText("Nhập ID");
			idField.getStyleClass().add("input-field");

			pinField = new PasswordField();
			pinField.setPromptText("Nhập Mã PIN");
			pinField.getStyleClass().add("input-field");

			Button loginButton = new Button("Đăng nhập");
			loginButton.setId("loginButton");

			loginButton.setOnAction(event -> {
				String enteredId = idField.getText();
				String enteredPin = pinField.getText();

				if (authenticateUser(enteredId, enteredPin)) {
					currentAccountId = enteredId;
					showATMPanel(root, primaryStage);
				} else {
					outputArea.setText("ID hoặc Mã PIN không chính xác!");
				}
			});

			outputArea = new TextArea();
			outputArea.setEditable(false);
			outputArea.getStyleClass().add("output-area");

			loginPanel.getChildren().addAll(title, idField, pinField, loginButton, outputArea);
			root.setCenter(loginPanel);
		}

		private boolean authenticateUser(String accountId, String pin) {
			try {
				String query = "SELECT * FROM accounts WHERE account_id = ? AND pin = ?";
				PreparedStatement stmt = connection.prepareStatement(query);
				stmt.setString(1, accountId);
				stmt.setString(2, pin);

				ResultSet rs = stmt.executeQuery();
				return rs.next();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return false;
		}

		public void showATMPanel(BorderPane root, Stage primaryStage) {
			VBox atmPanel = new VBox(10);
			atmPanel.setAlignment(Pos.CENTER);

			Text title = new Text("Chào mừng đến với ATM");
			title.getStyleClass().add("title");

			Button balanceButton = new Button("Xem Số Dư");
			Button historyButton = new Button("Lịch sử giao dịch");
			Button withdrawButton = new Button("Rút tiền");
			Button depositButton = new Button("Gửi tiền");
			Button exitButton = new Button("Thoát");

			balanceButton.setOnAction(e -> showBalance());
			historyButton.setOnAction(e -> showTransactionHistory());
			withdrawButton.setOnAction(e -> handleWithdraw());
			depositButton.setOnAction(e -> handleDeposit());
			exitButton.setOnAction(e -> primaryStage.close());

			atmPanel.getChildren().addAll(title, balanceButton, historyButton, withdrawButton, depositButton,
					exitButton, outputArea);
			root.setCenter(atmPanel);
		}

		private void showBalance() {
			try {
				String query = "SELECT balance FROM accounts WHERE account_id = ?";
				PreparedStatement stmt = connection.prepareStatement(query);
				stmt.setString(1, currentAccountId);

				ResultSet rs = stmt.executeQuery();
				if (rs.next()) {
					outputArea.setText("Số dư tài khoản hiện tại: " + rs.getBigDecimal("balance"));
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		private void showTransactionHistory() {
			try {
				String query = "SELECT transaction_history FROM accounts WHERE account_id = ?";
				PreparedStatement stmt = connection.prepareStatement(query);
				stmt.setString(1, currentAccountId);

				ResultSet rs = stmt.executeQuery();
				if (rs.next()) {
					String transactionHistory = rs.getString("transaction_history");

					// Kiểm tra nếu có lịch sử giao dịch
					if (transactionHistory != null && !transactionHistory.isEmpty()) {
						// Tạo đối tượng DecimalFormat để định dạng số tiền
						DecimalFormat df = new DecimalFormat("#,##0.00"); // Định dạng với phần nghìn và 2 chữ số thập
																			// phân

						// Chia tách các dòng giao dịch và định dạng số tiền
						String[] transactions = transactionHistory.split("\n");
						StringBuilder formattedHistory = new StringBuilder("Lịch sử giao dịch:\n");

						for (String transaction : transactions) {
							// Giả sử mỗi giao dịch có định dạng như "Rút tiền: -50000" hoặc "Gửi tiền:
							// +100000"
							String[] transactionParts = transaction.split(": ");
							if (transactionParts.length == 2) {
								String transactionType = transactionParts[0];
								String amountStr = transactionParts[1];

								// Định dạng số tiền
								try {
									double amount = Double.parseDouble(amountStr.replaceAll(",", "")); // Loại bỏ dấu
																										// phân cách nếu
																										// có
									String formattedAmount = df.format(amount);
									formattedHistory.append(transactionType).append(": ").append(formattedAmount)
											.append(" VND\n");
								} catch (NumberFormatException e) {
									// Nếu không thể định dạng số, chỉ cần thêm giao dịch không thay đổi
									formattedHistory.append(transaction).append("\n");
								}
							}
						}
						outputArea.setText(formattedHistory.toString());
					} else {
						outputArea.setText("Không có lịch sử giao dịch.");
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		private boolean checkBalance(double amount) {
		    try {
		        String query = "SELECT balance FROM accounts WHERE account_id = ?";
		        PreparedStatement stmt = connection.prepareStatement(query);
		        stmt.setString(1, currentAccountId);

		        ResultSet rs = stmt.executeQuery();
		        if (rs.next()) {
		            double currentBalance = rs.getDouble("balance");
		            return currentBalance >= amount;  // Kiểm tra nếu số dư hiện tại đủ cho giao dịch
		        }
		    } catch (SQLException e) {
		        e.printStackTrace();
		    }
		    return false;  // Nếu có lỗi, không đủ số dư
		}
 

		private void handleWithdraw() {
			TextInputDialog withdrawDialog = new TextInputDialog();
			withdrawDialog.setTitle("Rút tiền");
			withdrawDialog.setHeaderText("Nhập số tiền cần rút");

			withdrawDialog.showAndWait().ifPresent(input -> {
				try {
					double amount = Double.parseDouble(input);
					if (updateBalance(-amount)) {
						recordTransaction("Rút tiền: -" + amount);
						showBalance();
					} else { 
						outputArea.setText("Số dư không đủ.");
					}
				} catch (NumberFormatException e) {
					outputArea.setText("Vui lòng nhập số tiền hợp lệ.");
				}

			});
		}

		private void handleDeposit() {
		    TextInputDialog depositDialog = new TextInputDialog();
		    depositDialog.setTitle("Gửi tiền");
		    depositDialog.setHeaderText("Nhập số tiền cần gửi");

		    depositDialog.showAndWait().ifPresent(input -> {
		        try {
		            double amount = Double.parseDouble(input);

		            // Kiểm tra nếu số tiền gửi là bội số của 10000
		            if (amount % 10000 != 0) {
		                outputArea.setText("Vui lòng gửi tiền là bội số của 10.000.");
		            } else {
		                if (updateBalance(amount)) {
		                    recordTransaction("Gửi tiền: +" + amount);
		                    showBalance();
		                }
		            }
		        } catch (NumberFormatException e) {
		            outputArea.setText("Vui lòng nhập số tiền hợp lệ.");
		        }
		    });
		}


		private boolean updateBalance(double amount) {
			try {
				String query = "UPDATE accounts SET balance = balance + ? WHERE account_id = ?";
				PreparedStatement stmt = connection.prepareStatement(query);
				stmt.setDouble(1, amount);
				stmt.setString(2, currentAccountId);

				return stmt.executeUpdate() > 0;
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return false;
		}

		private void recordTransaction(String transaction) {
			try {
				String query = "UPDATE accounts SET transaction_history = CONCAT(IFNULL(transaction_history, ''), ?, '\n') WHERE account_id = ?";
				PreparedStatement stmt = connection.prepareStatement(query);
				stmt.setString(1, transaction);
				stmt.setString(2, currentAccountId);
				stmt.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}