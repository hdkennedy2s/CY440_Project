package org.todo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class ToDoList {

    private static int currentUserId;

    public static void setCurrentUserId(int userId) {
        currentUserId = userId;
    }

    public static void addTask(String task) {
        if (task == null || task.trim().isEmpty()) {
            System.out.println("Task cannot be empty.");
            return;
        }

        String sql = "INSERT INTO tasks (user_id, task) VALUES (?, ?)";
        try (Connection conn = Login.connect()) {
            assert conn != null;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, currentUserId);
                pstmt.setString(2, task.trim());
                pstmt.executeUpdate();
                System.out.println("Task added successfully.");
            }
        } catch (SQLException e) {
            System.out.println("Error adding task: " + e.getMessage());
        }
    }

    public static void viewTasks() {
        String sql = "SELECT id, task, completed FROM tasks WHERE user_id = ?";
        try (Connection conn = Login.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, currentUserId);
            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.println("Your tasks:");
                boolean hasTasks = false;
                while (rs.next()) {
                    hasTasks = true;
                    String status = rs.getBoolean("completed") ? "[Completed]" : "[Pending]";
                    System.out.println(rs.getInt("id") + ". " + rs.getString("task") + " " + status);
                }
                if (!hasTasks) {
                    System.out.println("No tasks found.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving tasks: " + e.getMessage());
        }
    }

    public static void markTaskAsCompleted(int taskId) {
        if (taskId <= 0) {
            System.out.println("Invalid task ID. Please enter a valid number.");
            return;
        }

        String sql = "UPDATE tasks SET completed = TRUE WHERE id = ? AND user_id = ?";
        try (Connection conn = Login.connect()) {
            assert conn != null;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, taskId);
                pstmt.setInt(2, currentUserId);
                int rows = pstmt.executeUpdate();
                if (rows > 0) {
                    System.out.println("Task marked as completed.");
                } else {
                    System.out.println("Task not found or you do not have access to this task.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error updating task: " + e.getMessage());
        }
    }

    public static void clearAllTasks() {
        String sql = "DELETE FROM tasks WHERE user_id = ?";
        try (Connection conn = Login.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, currentUserId);
            int rowsDeleted = pstmt.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("All tasks cleared successfully.");
            } else {
                System.out.println("No tasks to clear.");
            }
        } catch (SQLException e) {
            System.out.println("Error clearing tasks: " + e.getMessage());
        }
    }


    public static void mainMenu() {
        Scanner scanner = new Scanner(System.in);
        boolean runMenu = true;

        while (runMenu) {
            System.out.println("\nTo-Do List Menu:");
            System.out.println("1. Add Task\n2. View Tasks\n3. Mark Task as Completed\n4. Clear All Tasks\n0. Exit Program");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1": //Add Task
                    System.out.print("Enter task: ");
                    String task = scanner.nextLine().trim();
                    if (task.isEmpty()) {
                        System.out.println("Task cannot be empty.");
                    } else {
                        addTask(task);
                    }
                    break;

                case "2": //View Tasks
                    viewTasks();
                    break;

                case "3": //Mark Task as Completed
                    viewTasks();
                    System.out.print("Enter number ID of task to mark as completed: ");
                    try {
                        int taskId = Integer.parseInt(scanner.nextLine().trim());
                        markTaskAsCompleted(taskId);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please enter a valid task ID.");
                    }
                    break;

                case "4": //Clear All Tasks
                    clearAllTasks();
                    break;

                case "0": //Exit Program
                    System.out.println("Goodbye.");
                    System.exit(0);
                    break;

                default: //Retry
                    System.out.println("Invalid choice. Please enter a valid option (0-4).");
                    break;
            }
        }
    }
}
