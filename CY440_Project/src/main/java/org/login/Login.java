package org.login;

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
    private static final String DB_URL = "jdbc:mysql://ec2-18-224-110-32.us-east-2.compute.amazonaws.com:3306/cy440_project";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Pr0j3ctDB";

    // Method to establish a connection to the database
    public static Connection connect() {
        try {
            // Establish a connection to the MySQL database
            return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        } catch (SQLException e) {
            System.out.println("Error connecting to the database: " + e.getMessage());
            return null;  // Return null if the connection fails
        }
    }

    // Method to create a table in the database if it doesn't exist
    private static void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                " id INT AUTO_INCREMENT PRIMARY KEY," +
                " username VARCHAR(50) NOT NULL UNIQUE," +
                " password VARCHAR(64) NOT NULL," +
                " salt VARCHAR(24) NOT NULL" +
                ");";

        try (Connection conn = connect()) {
            assert conn != null;
            try (// Use the connect method
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.execute();
            }
        } catch (SQLException e) {
            System.out.println("Error finding or creating table: " + e.getMessage());
        }
    }

    // Method to hash the password with a salt
    private static String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(salt.getBytes());
        byte[] hash = digest.digest(password.getBytes());
        return Base64.getEncoder().encodeToString(hash);
    }

    // Method to generate a random salt
    private static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    // Method to insert a new user with hashed password
    public static void addUser(String username, String password) {
        String salt = generateSalt();

        try {
            String hashedPassword = hashPassword(password, salt);

            String sql = "INSERT INTO users (username, password, salt) VALUES (?, ?, ?)";
            try (Connection conn = connect()) {
                assert conn != null;
                try (// Use the connect method
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, username);
                    pstmt.setString(2, hashedPassword);
                    pstmt.setString(3, salt);
                    pstmt.executeUpdate();
                    System.out.println("User added successfully.");
                }
            } catch (SQLException e) {
                System.out.println("Error inserting user: " + e.getMessage());
            }
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error hashing password: " + e.getMessage());
        }
    }

    // Method to verify the user login
    public static boolean verifyUser(String username, String inputPassword) {
        String sql = "SELECT password, salt FROM users WHERE username = ?";

        try (Connection conn = connect()) {
            assert conn != null;
            try (// Use the connect method
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String storedHash = rs.getString("password");
                        String storedSalt = rs.getString("salt");

                        String inputHash = hashPassword(inputPassword, storedSalt);

                        if (storedHash.equals(inputHash)) {
                            return true;  // Login successful
                        }
                    }
                } catch (SQLException e) {
                    System.out.println("Error during query execution: " + e.getMessage());
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error connecting to the database: " + e.getMessage());
        }
        return false;  // Login failed
    }

    public static void login() {
        createTable();  // Ensures the table exists before starting login operations

        Scanner scanner = new Scanner(System.in);
        boolean runLoginMenu = true;

        System.out.println("Please select an option:");

        while (runLoginMenu) {
            System.out.println("1. Login as existing user\n" +
                    "2. Create User\n" +
                    "0. Exit\n");

            String loginMenuSelection = scanner.nextLine();

            switch (loginMenuSelection) {
                case "1":
                    System.out.print("Enter username: ");
                    String username = scanner.nextLine();
                    System.out.print("Enter password: ");
                    String inputPassword = scanner.nextLine();

                    if (verifyUser(username, inputPassword)) {
                        System.out.println("Login successful.");
                        runLoginMenu = false;  // Exit the menu loop after successful login
                    } else {
                        System.out.println("Login failed.");
                    }
                    break;

                case "2":
                    System.out.print("Enter username: ");
                    username = scanner.nextLine();
                    System.out.print("Enter password: ");
                    inputPassword = scanner.nextLine();

                    addUser(username, inputPassword);
                    break;

                case "0":
                    System.out.println("Goodbye.");
                    System.exit(0);  // Exit the application

                default:
                    System.out.println("Please enter a valid option.");
                    break;
            }
        }
    }
}
