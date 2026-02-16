package com.eventmanagement.utils;

import com.eventmanagement.models.Utilisateur;

public class Session {
    private static Session instance;
    private Utilisateur currentUser;
    
    private Session() {}
    
    public static Session getInstance() {
        if (instance == null) {
            instance = new Session();
        }
        return instance;
    }
    
    public void setCurrentUser(Utilisateur user) {
        this.currentUser = user;
    }
    
    public Utilisateur getCurrentUser() {
        return currentUser;
    }
    
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    public boolean isAdmin() {
        return currentUser != null && "ADMIN".equals(currentUser.getRole());
    }
    
    public void logout() {
        currentUser = null;
    }
}
