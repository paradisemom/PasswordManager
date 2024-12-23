package com.passwordmanager;
// src/MainApp.java
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import com.passwordmanager.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        LoginManager loginManager = new LoginManager();
        String username = null;

        String[] options = {"Login", "Register", "Guest Mode"};
        int choice = JOptionPane.showOptionDialog(null, "Welcome to Password Manager", "Login",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);

        if (choice == 0) { // Login
            username = attemptLogin(loginManager);
            if (username != null) {
                new GUIManager(new PasswordManager(), username);
            } else {
                System.exit(0); // Login failed, exit application
            }
        } else if (choice == 1) { // Register
            registerUser(loginManager);
            System.exit(0); // Exit after registration
        } else if (choice == 2) { // Guest Mode
            new GUIManager(new PasswordManager(), "guest");
        } else {
            System.exit(0); // Exit on close or cancel
        }
    }

    private static String attemptLogin(LoginManager loginManager) {
        String username = JOptionPane.showInputDialog("Enter Username:");
        String password = JOptionPane.showInputDialog("Enter Password:");
        if (loginManager.login(username, password)) {
            JOptionPane.showMessageDialog(null, "Login successful!");
            return username;
        } else {
            JOptionPane.showMessageDialog(null, "Login failed. Please try again.");
            return null;
        }
    }

    private static void registerUser(LoginManager loginManager) {
        String username = JOptionPane.showInputDialog("Choose Username:");
        String password = JOptionPane.showInputDialog("Choose Password:");
        if (loginManager.register(username, password)) {
            JOptionPane.showMessageDialog(null, "Registration successful!");
        } else {
            JOptionPane.showMessageDialog(null, "Username already exists.");
        }
    }
}