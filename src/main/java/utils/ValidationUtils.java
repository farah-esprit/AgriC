package utils;

import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import java.time.LocalDate;
import java.util.regex.Pattern;

public class ValidationUtils {

    // ═══════════════════════════════════════════════════════
    // PATTERNS
    // ═══════════════════════════════════════════════════════

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    // ✅ Format tunisien +216XXXXXXXX
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+216[0-9]{8}$"
    );

    private static final Pattern NAME_PATTERN = Pattern.compile(
            "^[A-Za-zÀ-ÿ\\s'-]{4,10}$"
    );

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Z])(?=.*[0-9])(?=.*[!@#$%^&*]).{8,}$"
    );

    // ═══════════════════════════════════════════════════════
    // VALIDATION DE BASE
    // ═══════════════════════════════════════════════════════

    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    public static boolean isValidName(String name) {
        return name != null && NAME_PATTERN.matcher(name).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }

    public static boolean isValidStrongPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        return hasUpper && hasLower && hasDigit;
    }

    public static boolean isValidCode(String code, int length) {
        return code != null && code.matches("\\d{" + length + "}");
    }

    public static boolean isValidLength(String value, int minLength, int maxLength) {
        return isNotEmpty(value) && value.length() >= minLength && value.length() <= maxLength;
    }

    public static boolean hasMinLength(String text, int minLength) {
        return text != null && text.length() >= minLength;
    }

    public static boolean hasMaxLength(String text, int maxLength) {
        return text != null && text.length() <= maxLength;
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

    // ═══════════════════════════════════════════════════════
    // VALIDATION DATES
    // ═══════════════════════════════════════════════════════

    public static boolean isValidDateRange(LocalDate startDate, LocalDate endDate) {
        return startDate != null && endDate != null && !endDate.isBefore(startDate);
    }

    public static boolean isDateInFuture(LocalDate date) {
        return date != null && date.isAfter(LocalDate.now());
    }

    public static boolean isDateInPast(LocalDate date) {
        return date != null && date.isBefore(LocalDate.now());
    }

    // ═══════════════════════════════════════════════════════
    // VALIDATION MÉTIER
    // ═══════════════════════════════════════════════════════

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
        if (!isValidStrongPassword(password))
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

    // ═══════════════════════════════════════════════════════
    // STYLES DES CHAMPS
    // ═══════════════════════════════════════════════════════

    public static void setFieldError(TextField field) {
        if (field != null) field.setStyle("-fx-border-color: #d32f2f; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");
    }

    public static void setFieldError(PasswordField field) {
        if (field != null) field.setStyle("-fx-border-color: #d32f2f; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");
    }

    public static void setFieldError(TextArea field) {
        if (field != null) field.setStyle("-fx-border-color: #d32f2f; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");
    }

    public static void setFieldSuccess(TextField field) {
        if (field != null) field.setStyle("-fx-border-color: #4caf50; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");
    }

    public static void setFieldSuccess(PasswordField field) {
        if (field != null) field.setStyle("-fx-border-color: #4caf50; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");
    }

    public static void resetFieldStyle(TextField field) {
        if (field != null) field.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");
    }

    public static void resetFieldStyle(PasswordField field) {
        if (field != null) field.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");
    }

    public static void resetFieldStyle(TextArea field) {
        if (field != null) field.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");
    }

    // ═══════════════════════════════════════════════════════
    // AFFICHAGE MESSAGES LABEL
    // ═══════════════════════════════════════════════════════

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

    public static void showInfo(Label label, String message) {
        if (label != null) {
            label.setText("ℹ️ " + message);
            label.setStyle("-fx-text-fill: #2196f3; -fx-font-weight: bold;");
        }
    }

    public static void clearMessage(Label label) {
        if (label != null) label.setText("");
    }

    // ✅ Signature avec Control + Label (depuis code 1)
    public static void showError(Control control, Label errorLabel, String message) {
        if (control instanceof TextField)
            ((TextField) control).getStyleClass().add("text-field-error");
        else if (control instanceof TextArea)
            ((TextArea) control).getStyleClass().add("text-field-error");

        if (errorLabel != null) {
            errorLabel.setText(message);
            errorLabel.setStyle("-fx-text-fill: #dc3545;");
            errorLabel.setVisible(true);
        }
    }

    public static void clearError(Control control, Label errorLabel) {
        if (control instanceof TextField)
            ((TextField) control).getStyleClass().remove("text-field-error");
        else if (control instanceof TextArea)
            ((TextArea) control).getStyleClass().remove("text-field-error");

        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
        }
    }

    // ═══════════════════════════════════════════════════════
    // SANITIZE
    // ═══════════════════════════════════════════════════════

    public static String sanitize(String input) {
        if (input == null) return "";
        return input.trim().replaceAll("\\s+", " ");
    }

    // ═══════════════════════════════════════════════════════
    // VALIDATION RESULT
    // ═══════════════════════════════════════════════════════

    public static class ValidationResult {
        private final boolean isValid;
        private final String  errorMessage;

        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid       = isValid;
            this.errorMessage  = errorMessage;
        }

        public boolean isValid()        { return isValid; }
        public String  getErrorMessage(){ return errorMessage; }
    }
}