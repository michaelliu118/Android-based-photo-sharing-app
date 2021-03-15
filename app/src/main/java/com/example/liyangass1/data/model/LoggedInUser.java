package com.example.liyangass1.data.model;

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
public class LoggedInUser {

    private String userId;
    private String displayName;
    private String shortBio;

    public LoggedInUser(String userId, String displayName, String shortBio) {
        this.userId = userId;
        this.displayName = displayName;
        this.shortBio = shortBio;
    }

    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getShortBio(){ return shortBio;}
}