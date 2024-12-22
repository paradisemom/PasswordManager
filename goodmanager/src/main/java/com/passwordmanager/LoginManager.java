package com.passwordmanager;

import javax.swing.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import com.google.gson.Gson;

public class LoginManager {
    private static final String USER_DATA_FILE = "users.json";
    private Map<String, String> userData;

    public LoginManager() {
        userData = loadUserData();
    }

    public boolean register(String username, String password) {
        if (userData.containsKey(username)) return false;
        userData.put(username, password);
        saveUserData();
        return true;
    }

    public boolean login(String username, String password) {
        return userData.containsKey(username) && userData.get(username).equals(password);
    }

    private Map<String, String> loadUserData() {
        try (Reader reader = new FileReader(USER_DATA_FILE)) {
            return new Gson().fromJson(reader, Map.class);
        } catch (IOException e) {
            return new HashMap<>();
        }
    }

    private void saveUserData() {
        try (Writer writer = new FileWriter(USER_DATA_FILE)) {
            new Gson().toJson(userData, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}