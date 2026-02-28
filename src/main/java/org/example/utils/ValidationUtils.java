package org.example.utils;


import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import java.util.regex.Pattern;

public class ValidationUtils {
    // Patterns de validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    // ✅ CORRIGÉ : Format tunisien +216XXXXXXXX
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+216[0-9]{8}$"
    );

    private static final Pattern NAME_PATTERN = Pattern.compile(
            "^[A-Za-zÀ-ÿ\\s'-]{4,10}$"
    );

    // ================= VALIDATION EMAIL =================
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    // ================= VALIDATION TÉLÉPHONE =================
    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    // ================= VALIDATION NOM =================
    public static boolean isValidName(String name) {
        return name != null && NAME_PATTERN.matcher(name).matches();
    }

    // ================= VALIDATION MOT DE PASSE =================
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }

    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) return false;

        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);

        return hasUpper && hasLower && hasDigit;
    }

    // ================= VALIDATION CODE =================
    public static boolean isValidCode(String code, int length) {
        return code != null && code.matches("\\d{" + length + "}");
    }

    // ================= VALIDATION CHAMP VIDE =================
    public static boolean isNotEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }

    // ================= VALIDATION LONGUEUR =================
    public static boolean hasMinLength(String text, int minLength) {
        return text != null && text.length() >= minLength;
    }

    public static boolean hasMaxLength(String text, int maxLength) {
        return text != null && text.length() <= maxLength;
    }

    // ================= STYLE DES CHAMPS =================
    public static void setFieldError(TextField field) {
        field.setStyle("-fx-border-color: #d32f2f; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");
    }

    public static void setFieldError(PasswordField field) {
        field.setStyle("-fx-border-color: #d32f2f; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");
    }

    public static void setFieldError(TextArea field) {
        field.setStyle("-fx-border-color: #d32f2f; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");
    }

    public static void setFieldSuccess(TextField field) {
        field.setStyle("-fx-border-color: #4caf50; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");
    }

    public static void setFieldSuccess(PasswordField field) {
        field.setStyle("-fx-border-color: #4caf50; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");
    }

    public static void resetFieldStyle(TextField field) {
        field.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");
    }

    public static void resetFieldStyle(PasswordField field) {
        field.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");
    }

    public static void resetFieldStyle(TextArea field) {
        field.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");
    }

    // ================= AFFICHAGE MESSAGES =================
    public static void showError(Label label, String message) {
        if (label != null) {
            label.setText("❌ " + message);
            label.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
        }
    }

    public static void showSuccess(Label label, String message) {
        if (label != null) {
            label.setText("✅ " + message);
            label.setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;");
        }
    }

    public static void showWarning(Label label, String message) {
        if (label != null) {
            label.setText("⚠️ " + message);
            label.setStyle("-fx-text-fill: #ff9800; -fx-font-weight: bold;");
        }
    }

    // ✅ AJOUTÉ
    public static void showInfo(Label label, String message) {
        if (label != null) {
            label.setText("ℹ️ " + message);
            label.setStyle("-fx-text-fill: #2196f3; -fx-font-weight: bold;");
        }
    }

    public static void clearMessage(Label label) {
        if (label != null) {
            label.setText("");
        }
    }

    // ================= VALIDATION COMPLÈTE FORMULAIRE =================
    public static class ValidationResult {
        private boolean isValid;
        private String errorMessage;

        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() {
            return isValid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    // ================= SANITIZE INPUT =================
    public static String sanitize(String input) {
        if (input == null) return "";
        return input.trim().replaceAll("\\s+", " ");
    }
}
