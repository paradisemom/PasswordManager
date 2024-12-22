package com.passwordmanager;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.io.*;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class PasswordManager {
    private List<PasswordEntry> passwordList;
    private Map<String, PasswordEntry> passwordMap; // Track by site + account

    public PasswordManager() {
        passwordList = new ArrayList<>();
        passwordMap = new HashMap<>(); // Initialize detailed tracker
    }

    public void addPassword(String site, String account, String password, String category, String encryptionKey) throws Exception {
        String key = site + "|" + account;
        String encryptedPassword = Utils.encrypt(password, encryptionKey);
        PasswordEntry entry = new PasswordEntry(site, account, encryptedPassword, category);
        passwordList.add(entry);
        passwordMap.put(key, entry); // Track unique site + account
    }

    public List<PasswordEntry> searchByCategory(String category) {
        List<PasswordEntry> results = new ArrayList<>();
        for (PasswordEntry entry : passwordList) {
            if (entry.getCategory().equalsIgnoreCase(category)) {
                results.add(entry);
            }
        }
        return results;
    }

    public void updatePassword(String site, String account, String newPassword, String newCategory, String encryptionKey) throws Exception {
        String key = site + "|" + account;
        if (passwordMap.containsKey(key)) {
            PasswordEntry entry = passwordMap.get(key);
            // Update encrypted password and category
            entry.setPassword(Utils.encrypt(newPassword, encryptionKey));
            entry.setCategory(newCategory);
        }
        savePasswords("passwords.json", encryptionKey); // Save changes after update
    }

    public boolean isDuplicate(String site, String account) {
        return passwordMap.containsKey(site + "|" + account);
    }

    public boolean isPasswordMismatch(String site, String account, String password) {
        PasswordEntry existing = passwordMap.get(site + "|" + account);
        return existing != null && !existing.getPassword().equals(password);
    }

    public PasswordEntry getExistingEntry(String site, String account) {
        return passwordMap.get(site + "|" + account);
    }

    public void deletePassword(int index) {
        if (index >= 0 && index < passwordList.size()) {
            PasswordEntry entry = passwordList.remove(index);
            passwordMap.remove(entry.getSite() + "|" + entry.getAccount()); // Remove from map
        } else {
            throw new IndexOutOfBoundsException("Invalid index position!");
        }
    }

    public List<PasswordEntry> getPasswords() {
        return passwordList;
    }

    public List<PasswordEntry> searchPasswords(String query) {
        List<PasswordEntry> results = new ArrayList<>();
        for (PasswordEntry entry : passwordList) {
            if (entry.getSite().contains(query) || entry.getAccount().contains(query)) {
                results.add(entry);
            }
        }
        return results;
    }

    public void loadPasswords(String filePath, String encryptionKey) {
        try (Reader reader = new FileReader(filePath)) {
            Gson gson = new Gson();
            passwordList = gson.fromJson(reader, new TypeToken<List<PasswordEntry>>() {}.getType());
            passwordMap.clear();
            for (PasswordEntry entry : passwordList) {
                // 解密存儲密碼
                try {
                    entry.password = Utils.decrypt(entry.password, encryptionKey);
                } catch (Exception e) {
                    entry.password = "ERROR";
                }
                passwordMap.put(entry.getSite() + "|" + entry.getAccount(), entry);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deletePassword(String site, String account) {
        String key = site + "|" + account;
        passwordList.removeIf(entry -> entry.getSite().equals(site) && entry.getAccount().equals(account));
        passwordMap.remove(key);
    }

    public void savePasswords(String filePath, String encryptionKey) {
        try (Writer writer = new FileWriter(filePath)) {
            Gson gson = new Gson();
            List<PasswordEntry> encryptedList = new ArrayList<>();
            for (PasswordEntry entry : passwordList) {
                encryptedList.add(new PasswordEntry(
                    entry.getSite(),
                    entry.getAccount(),
                    Utils.encrypt(entry.getPassword(), encryptionKey),
                    entry.getCategory()
                ));
            }
            gson.toJson(encryptedList, writer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class PasswordEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        private String site;
        private String account;
        private String password;
        private String category;

        public PasswordEntry(String site, String account, String password, String category) {
            this.site = site;
            this.account = account;
            this.password = password;
            this.category = category;
        }

        public String getSite() {
            return site;
        }

        public String getAccount() {
            return account;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
        public String getCategory() {
            return category;
        }
        public void setCategory(String category) {
            this.category = category;
        }
    }
}
