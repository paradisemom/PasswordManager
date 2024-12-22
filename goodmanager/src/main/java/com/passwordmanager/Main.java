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
        SwingUtilities.invokeLater(() -> new GUIManager(new PasswordManager()));
    }
}
