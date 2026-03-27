package service;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordService {

    // Hasher un mot de passe
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    // Vérifier un mot de passe
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}