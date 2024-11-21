package org.todo;

import static org.todo.Login.login;
import static org.todo.ToDoList.mainMenu;

public class Main {

    public static void main(String[] args) {
        login(); //Initiate Login
        mainMenu(); //Initiate Main To-Do List Menu
    }
}
