package org.todo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Scanner;

public class Login {

    // MySQL database credentials
    private static final String DB_URL = "jdbc:mysql://ec2-18-221-154-200.us-east-2.compute.amazonaws.com:3306/cy440_project";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Pr0j3ctDB";

    // Method to establish a connection to the database
    public static Connection connect() {
        try {
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            System.out.println("Error connecting to the database: " + e.getMessage());
            return null;
        }
    }

    // Create the users table
    private static void createUserTable() {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                " id INT AUTO_INCREMENT PRIMARY KEY," +
                " username VARCHAR(50) NOT NULL UNIQUE," +
                " password VARCHAR(64) NOT NULL," +
                " salt VARCHAR(24) NOT NULL" +
                ");";

        try (Connection conn = connect()) {
            if (conn != null) {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.execute();
                }
            }
        } catch (SQLException e) {
            System.out.println("Error creating users table: " + e.getMessage());
        }
    }

    // Create the tasks table
    private static void createTaskTable() {
        String sql = "CREATE TABLE IF NOT EXISTS tasks (" +
                " id INT AUTO_INCREMENT PRIMARY KEY," +
                " user_id INT NOT NULL," +
                " task VARCHAR(255) NOT NULL," +
                " completed BOOLEAN DEFAULT FALSE," +
                " FOREIGN KEY (user_id) REFERENCES users(id)" +
                ");";

        try (Connection conn = connect()) {
            if (conn != null) {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.execute();
                }
            }
        } catch (SQLException e) {
            System.out.println("Error creating tasks table: " + e.getMessage());
        }
    }

    // Hash the password with a salt
    private static String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(salt.getBytes());
        byte[] hash = digest.digest(password.getBytes());
        return Base64.getEncoder().encodeToString(hash);
    }

    // Generate a random salt
    private static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    // Add a new user
    public static void addUser(String username, String password) {
        String salt = generateSalt();

        try {
            String hashedPassword = hashPassword(password, salt);

            String sql = "INSERT INTO users (username, password, salt) VALUES (?, ?, ?)";
            try (Connection conn = connect()) {
                assert conn != null;
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, username);
                    pstmt.setString(2, hashedPassword);
                    pstmt.setString(3, salt);
                    pstmt.executeUpdate();
                    System.out.println("User added successfully.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error inserting user: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error hashing password: " + e.getMessage());
        }
    }

    // Verify the user login
    public static int verifyUser(String username, String inputPassword) {
        String sql = "SELECT id, password, salt FROM users WHERE username = ?";
        try (Connection conn = connect()) {
            assert conn != null;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String storedHash = rs.getString("password");
                        String storedSalt = rs.getString("salt");
                        int userId = rs.getInt("id");

                        String inputHash = hashPassword(inputPassword, storedSalt);
                        if (storedHash.equals(inputHash)) {
                            return userId; // Return user ID on success
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.out.println("Error during query execution: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error hashing password: " + e.getMessage());
        }
        return -1; // Return -1 on failure
    }

    // Login menu
    public static void login() {
        createUserTable();
        createTaskTable();

        Scanner scanner = new Scanner(System.in);
        boolean runLoginMenu = true;

        while (runLoginMenu) {
            System.out.println("\nPlease select an option:");
            System.out.println("1. Login as existing user\n2. Create User\n0. Exit");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    System.out.print("Enter username: ");
                    String username = scanner.nextLine();
                    System.out.print("Enter password: ");
                    String password = scanner.nextLine();

                    int userId = verifyUser(username, password);
                    if (userId != -1) {
                        System.out.println("Login successful.");
                        ToDoList.setCurrentUserId(userId); // Pass user ID to the To-Do List
                        ToDoList.mainMenu();
                    } else {
                        System.out.println("Login failed. Please try again.");
                    }
                    break;

                case "2":
                    System.out.print("Enter username: ");
                    username = scanner.nextLine();
                    System.out.print("Enter password: ");
                    password = scanner.nextLine();
                    addUser(username, password);
                    break;

                case "0":
                    System.out.println("Goodbye.");
                    runLoginMenu = false;
                    break;

                default:
                    System.out.println("Invalid option. Please try again.");
                    break;
            }
        }
    }
}
