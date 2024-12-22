package com.passwordmanager;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import com.passwordmanager.*;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.io.*;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class GUIManager extends JFrame {
    private PasswordManager manager;
    private JTable table;
    private DefaultTableModel tableModel;
    private static final String PASSWORD_FILE = "passwords.json";
    private static final String KEY_FILE = "key.dat";
    private static final String EXPORT_FILE = "exported_passwords.csv";
    private static final String IMPORT_FILE = "imported_passwords.csv";
    private String encryptionKey = "defaultEncryptionKey123456"; // Simplified encryption key
    private static final String[] CATEGORIES = {"Games", "Search Engines", "Social Media", "Streaming", "Others"};

    public GUIManager(PasswordManager manager) {
        this.manager = manager;
        setTitle("Password Manager");
        setSize(1000, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            encryptionKey = loadKey();
            if (encryptionKey == null) {
                encryptionKey = Utils.generateAESKey();
                saveKey(encryptionKey);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Failed to initialize encryption key!", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        tableModel = new DefaultTableModel(new String[]{"Site", "Account", "Password", "Category"}, 0);
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        JButton addButton = new JButton("Add");
        JButton deleteButton = new JButton("Delete");
        JButton generateButton = new JButton("Generate & Add");
        JButton updateButton = new JButton("Update");
        JButton searchButton = new JButton("Search");
        JButton searchCategoryButton = new JButton("Search by Category");
        JButton showAllButton = new JButton("Show All");
        JButton exportButton = new JButton("Export");
        JButton importButton = new JButton("Import");

        addButton.addActionListener(e -> addPassword());
        deleteButton.addActionListener(e -> deletePassword());
        generateButton.addActionListener(e -> generateAndAddPassword());
        updateButton.addActionListener(e -> updatePassword());
        searchButton.addActionListener(e -> searchPasswords());
        searchCategoryButton.addActionListener(e -> searchByCategory());
        showAllButton.addActionListener(e -> loadAllPasswords());
        exportButton.addActionListener(e -> exportPasswords());
        importButton.addActionListener(e -> importPasswords());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(generateButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(searchButton);
        buttonPanel.add(searchCategoryButton);
        buttonPanel.add(showAllButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(importButton);

        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        manager.loadPasswords(PASSWORD_FILE, encryptionKey);
        setVisible(true);
    }
    
    private void addPassword() {
        JTextField siteField = new JTextField();
        JTextField accountField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JComboBox<String> categoryBox = new JComboBox<>(CATEGORIES);

        Object[] fields = {
            "Site:", siteField,
            "Account:", accountField,
            "Password:", passwordField,
            "Category:", categoryBox
        };

        int option = JOptionPane.showConfirmDialog(this, fields, "Add Password", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                String site = siteField.getText();
                String account = accountField.getText();
                String password = new String(passwordField.getPassword());
                String category = (String) categoryBox.getSelectedItem();

                if (manager.isDuplicate(site, account)) {
                    JOptionPane.showMessageDialog(this, "Duplicate entry detected!", "Warning", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                manager.addPassword(site, account, password, category, encryptionKey);
                manager.savePasswords(PASSWORD_FILE,encryptionKey);
                loadAllPasswords();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to save password!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    private void deletePassword() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            String site = (String) tableModel.getValueAt(selectedRow, 0);
            String account = (String) tableModel.getValueAt(selectedRow, 1);
            manager.deletePassword(site, account);
            manager.savePasswords(PASSWORD_FILE,encryptionKey);
            updateTable();
        } else {
            JOptionPane.showMessageDialog(this, "Please select a row to delete.");
        }
    }
    private void updatePassword() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "No password selected!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JTextField siteField = new JTextField((String) tableModel.getValueAt(selectedRow, 0));
        JTextField accountField = new JTextField((String) tableModel.getValueAt(selectedRow, 1));
        JTextField passwordField = new JTextField();
        JComboBox<String> categoryBox = new JComboBox<>(CATEGORIES);
        categoryBox.setSelectedItem(tableModel.getValueAt(selectedRow, 3));

        Object[] fields = {
            "Site:", siteField,
            "Account:", accountField,
            "New Password:", passwordField,
            "Category:", categoryBox
        };

        int option = JOptionPane.showConfirmDialog(this, fields, "Update Password", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                String site = siteField.getText();
                String account = accountField.getText();
                String newPassword = passwordField.getText();
                String category = (String) categoryBox.getSelectedItem();

                if (!manager.isDuplicate(site, account)) {
                    JOptionPane.showMessageDialog(this, "Entry does not exist!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // 更新密碼但不存檔
                manager.updatePassword(site, account, newPassword, category, encryptionKey);
                loadAllPasswords(); // 更新顯示但不儲存
                JOptionPane.showMessageDialog(this, "Password updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to update password!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    private void loadAllPasswords() {
        tableModel.setRowCount(0);
        for (PasswordManager.PasswordEntry entry : manager.getPasswords()) {
            try {
                String decryptedPassword = Utils.decrypt(entry.getPassword(), encryptionKey);
                tableModel.addRow(new Object[]{entry.getSite(), entry.getAccount(), decryptedPassword, entry.getCategory()});
            } catch (Exception e) {
                tableModel.addRow(new Object[]{entry.getSite(), entry.getAccount(), entry.getPassword(), entry.getCategory()});
            }
        }
    }
    private void searchByCategory() {
        String category = (String) JOptionPane.showInputDialog(this, "Select category:", "Search by Category",
                JOptionPane.QUESTION_MESSAGE, null, CATEGORIES, CATEGORIES[0]);
        if (category != null) {
            tableModel.setRowCount(0);
            for (PasswordManager.PasswordEntry entry : manager.getPasswords()) {
                if (entry.getCategory().equalsIgnoreCase(category)) {
                    try {
                        String decryptedPassword = Utils.decrypt(entry.getPassword(), encryptionKey);
                        tableModel.addRow(new Object[]{entry.getSite(), entry.getAccount(), decryptedPassword, entry.getCategory()});
                    } catch (Exception e) {
                        tableModel.addRow(new Object[]{entry.getSite(), entry.getAccount(), entry.getPassword(), entry.getCategory()});
                    }
                }
            }
        }
    }


    private void searchPasswords() {
        String keyword = JOptionPane.showInputDialog(this, "Enter keyword to search:", "Search Passwords", JOptionPane.QUESTION_MESSAGE);
        if (keyword != null && !keyword.isEmpty()) {
            tableModel.setRowCount(0);
            for (PasswordManager.PasswordEntry entry : manager.getPasswords()) {
                if (entry.getSite().contains(keyword) || entry.getAccount().contains(keyword)) {
                    try {
                        String decryptedPassword = Utils.decrypt(entry.getPassword(), encryptionKey);
                        tableModel.addRow(new Object[]{entry.getSite(), entry.getAccount(), decryptedPassword, entry.getCategory()});
                    } catch (Exception e) {
                        tableModel.addRow(new Object[]{entry.getSite(), entry.getAccount(), entry.getPassword(), entry.getCategory()});
                    }
                }
            }
        }
    }
    private void generateAndAddPassword() {
        JTextField siteField = new JTextField();
        JTextField accountField = new JTextField();
        JComboBox<String> categoryBox = new JComboBox<>(CATEGORIES);
        JSpinner lengthSpinner = new JSpinner(new SpinnerNumberModel(12, 4, 64, 1));
        JCheckBox includeUppercase = new JCheckBox("Uppercase", true);
        JCheckBox includeLowercase = new JCheckBox("Lowercase", true);
        JCheckBox includeDigits = new JCheckBox("Digits", true);
        JCheckBox includeSpecialChars = new JCheckBox("Special Characters", false);
        JLabel strengthLabel = new JLabel("Password Strength: ");
        JProgressBar strengthBar = new JProgressBar(0, 100);
    
        JTextField passwordPreview = new JTextField();
        passwordPreview.setEditable(false);
    
        Object[] fields = {
            "Site:", siteField,
            "Account:", accountField,
            "Category:", categoryBox,
            "Password Length:", lengthSpinner,
            "Include:", includeUppercase, includeLowercase, includeDigits, includeSpecialChars,
            "Password Preview:", passwordPreview,
            strengthLabel, strengthBar
        };
    
        updateGeneratedPassword(passwordPreview, strengthBar, lengthSpinner, includeUppercase, includeLowercase, includeDigits, includeSpecialChars);
    
        int option = JOptionPane.showConfirmDialog(this, fields, "Generate Password", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                String site = siteField.getText();
                String account = accountField.getText();
                String category = (String) categoryBox.getSelectedItem();
                String generatedPassword = passwordPreview.getText();
    
                // 檢查重複資料
                if (manager.isDuplicate(site, account)) {
                    JOptionPane.showMessageDialog(this, "Duplicate entry detected!", "Warning", JOptionPane.WARNING_MESSAGE);
                    return; // 跳過重複項目
                }
    
                // 添加資料但不存檔
                manager.addPassword(site, account, generatedPassword, category, encryptionKey);
                loadAllPasswords(); // 更新顯示但不儲存
                JOptionPane.showMessageDialog(this, "Generated Password: " + generatedPassword, "Success", JOptionPane.INFORMATION_MESSAGE);
    
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to generate and add password!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    private void updateGeneratedPassword(JTextField preview, JProgressBar strengthBar, JSpinner length, JCheckBox upper, JCheckBox lower, JCheckBox digits, JCheckBox special) {
        int len = (int) length.getValue();
        String password = Utils.generatePassword(len, upper.isSelected(), lower.isSelected(), digits.isSelected(), special.isSelected());
        preview.setText(password);
        int strength = Utils.calculateStrength(password);
        strengthBar.setValue(strength);
        strengthBar.setString(strength + "%");
    }
    private void updateTable() {
        loadAllPasswords();
    }

    private void savePasswords() {
        try (Writer writer = new FileWriter(PASSWORD_FILE)) {
            Gson gson = new Gson();
            gson.toJson(manager.getPasswords(), writer);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to save passwords!", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // private void loadPasswords() {
    //     try (Reader reader = new FileReader(PASSWORD_FILE)) {
    //         List<PasswordManager.PasswordEntry> passwords = new Gson().fromJson(reader, new TypeToken<List<PasswordManager.PasswordEntry>>() {}.getType());
    //         manager.loadPasswords(passwords);
    //         updateTable();
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     }
    // }
    private void exportPasswords() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Export File");
        fileChooser.setSelectedFile(new File("exported_passwords.csv"));
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try (PrintWriter writer = new PrintWriter(fileToSave)) {
                writer.println("Site,Account,Password,Category");
                for (PasswordManager.PasswordEntry entry : manager.getPasswords()) {
                    try {
                        String decryptedPassword = Utils.decrypt(entry.getPassword(), encryptionKey);
                        writer.println(entry.getSite() + "," + entry.getAccount() + "," + decryptedPassword + "," + entry.getCategory());
                    } catch (Exception e) {
                        writer.println(entry.getSite() + "," + entry.getAccount() + "," + entry.getPassword() + "," + entry.getCategory());
                    }
                }
                JOptionPane.showMessageDialog(this, "Passwords exported successfully!", "Export", JOptionPane.INFORMATION_MESSAGE);
            } catch (FileNotFoundException e) {
                JOptionPane.showMessageDialog(this, "Failed to export passwords!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    private void importPasswords() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Import File");
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToImport = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(fileToImport))) {
                String line;
                boolean isHeader = true;
                while ((line = reader.readLine()) != null) {
                    if (isHeader) {
                        isHeader = false; // Skip header row
                        continue;
                    }
                    String[] parts = line.split(",");
                    if (parts.length == 4) {
                        String site = parts[0];
                        String account = parts[1];
                        String password = parts[2];
                        String category = parts[3];
                        // 檢查是否重複
                        if (manager.isDuplicate(site, account)) {
                            // 跳過重複項目
                            continue;
                        }
                        manager.addPassword(site, account, password, category, encryptionKey);
                    }
                }
                savePasswords();
                loadAllPasswords();
                JOptionPane.showMessageDialog(this, "Passwords imported successfully!", "Import", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to import passwords!", "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private void saveKey(String key) throws IOException {
        try (FileWriter writer = new FileWriter(KEY_FILE)) {
            writer.write(key);
        }
    }

    private String loadKey() throws IOException {
        File file = new File(KEY_FILE);
        if (!file.exists()) return null;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return reader.readLine();
        }
    }
}
