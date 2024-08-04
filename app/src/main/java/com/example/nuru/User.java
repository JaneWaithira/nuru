package com.example.nuru;

public class User {
    private String username;
    private String email;
    private String profilePicUrl;

    public User() {
        // Default constructor required for Firestore
    }

    public User(String username, String email, String profilePicUrl) {
        this.username = username;
        this.email = email;
        this.profilePicUrl = profilePicUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getProfilePicUrl() {
        return profilePicUrl;
    }

    public void setProfilePicUrl(String profilePicUrl) {
        this.profilePicUrl = profilePicUrl;
    }
}
