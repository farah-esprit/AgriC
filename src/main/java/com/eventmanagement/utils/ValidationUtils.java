package com.eventmanagement.utils;

import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import java.time.LocalDate;
import java.util.regex.Pattern;

public class ValidationUtils {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@(.+)$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[0-9]{8,15}$"
    );
    
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*]).{8,}$"
    );
    
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }
    
    public static boolean isValidEmail(String email) {
        return isNotEmpty(email) && EMAIL_PATTERN.matcher(email).matches();
    }
    
    public static boolean isValidPhone(String phone) {
        return isNotEmpty(phone) && PHONE_PATTERN.matcher(phone).matches();
    }
    
    public static boolean isValidPassword(String password) {
        return isNotEmpty(password) && PASSWORD_PATTERN.matcher(password).matches();
    }
    
    public static boolean isValidLength(String value, int minLength, int maxLength) {
        return isNotEmpty(value) && value.length() >= minLength && value.length() <= maxLength;
    }
    
    public static boolean isValidNumber(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static boolean isValidPositiveNumber(String value) {
        try {
            return Integer.parseInt(value) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static boolean isValidDateRange(LocalDate startDate, LocalDate endDate) {
        return startDate != null && endDate != null && !endDate.isBefore(startDate);
    }
    
    public static boolean isDateInFuture(LocalDate date) {
        return date != null && date.isAfter(LocalDate.now());
    }
    
    public static boolean isDateInPast(LocalDate date) {
        return date != null && date.isBefore(LocalDate.now());
    }
    
    public static String validateEventTitle(String title) {
        if (!isNotEmpty(title)) return "Le titre ne peut pas être vide";
        if (!isValidLength(title, 3, 200)) return "Le titre doit contenir entre 3 et 200 caractères";
        return null;
    }
    
    public static String validateEventDescription(String description) {
        if (!isNotEmpty(description)) return "La description ne peut pas être vide";
        if (description.length() < 10) return "La description doit contenir au moins 10 caractères";
        return null;
    }
    
    public static String validateEventLocation(String location) {
        if (!isNotEmpty(location)) return "Le lieu ne peut pas être vide";
        if (!isValidLength(location, 3, 200)) return "Le lieu doit contenir entre 3 et 200 caractères";
        return null;
    }
    
    public static String validateCapacity(String capacity) {
        if (!isNotEmpty(capacity)) return "La capacité ne peut pas être vide";
        if (!isValidPositiveNumber(capacity)) return "La capacité doit être un nombre positif";
        return null;
    }
    
    public static String validateEventDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) return "Les dates ne peuvent pas être vides";
        if (!isValidDateRange(startDate, endDate)) return "La date de fin doit être après la date de début";
        if (!isDateInFuture(startDate)) return "L'événement doit être prévu pour le futur";
        return null;
    }
    
    public static String validateComplaintSubject(String subject) {
        if (!isNotEmpty(subject)) return "L'objet ne peut pas être vide";
        if (!isValidLength(subject, 5, 200)) return "L'objet doit contenir entre 5 et 200 caractères";
        return null;
    }
    
    public static String validateComplaintDescription(String description) {
        if (!isNotEmpty(description)) return "La description ne peut pas être vide";
        if (description.length() < 10) return "La description doit contenir au moins 10 caractères";
        return null;
    }
    
    public static String validateUsername(String username) {
        if (!isNotEmpty(username)) return "Le nom d'utilisateur ne peut pas être vide";
        if (!isValidLength(username, 3, 100)) return "Le nom doit contenir entre 3 et 100 caractères";
        return null;
    }
    
    public static String validateEmail(String email) {
        if (!isNotEmpty(email)) return "L'email ne peut pas être vide";
        if (!isValidEmail(email)) return "Veuillez entrer un email valide";
        return null;
    }
    
    public static String validatePassword(String password) {
        if (!isNotEmpty(password)) return "Le mot de passe ne peut pas être vide";
        if (!isValidPassword(password)) 
            return "Le mot de passe doit contenir au moins 8 caractères, 1 majuscule, 1 chiffre et 1 caractère spécial";
        return null;
    }
    
    public static String validatePhone(String phone) {
        if (!isNotEmpty(phone)) return "Le téléphone ne peut pas être vide";
        if (!isValidPhone(phone)) return "Veuillez entrer un numéro de téléphone valide";
        return null;
    }
    
    public static String validateAddress(String address) {
        if (!isNotEmpty(address)) return "L'adresse ne peut pas être vide";
        if (!isValidLength(address, 5, 300)) return "L'adresse doit contenir entre 5 et 300 caractères";
        return null;
    }
    
    public static void showError(Control control, Label errorLabel, String message) {
        if (control instanceof TextField) {
            ((TextField) control).getStyleClass().add("text-field-error");
        } else if (control instanceof TextArea) {
            ((TextArea) control).getStyleClass().add("text-field-error");
        }
        
        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: #dc3545;");
            errorLabel.setVisible(true);
        }
    }
    
    public static void clearError(Control control, Label errorLabel) {
        if (control instanceof TextField) {
            ((TextField) control).getStyleClass().remove("text-field-error");
        } else if (control instanceof TextArea) {
            ((TextArea) control).getStyleClass().remove("text-field-error");
        }
        
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
        }
    }
}
