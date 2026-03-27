package main;

import entities.*;
import service.*;
import java.sql.SQLException;

public class Main {

    public static void main(String[] args) {

        // Services
        UserService userService = new UserService();
        ProfilService profilService = new ProfilService();

        System.out.println("========= TEST CRUD USER =========");

        // Création d'un User
        User u1 = new User(
                "Fatma",
                "fatma_test@gmail.com",
                "123456",
                Role.ADMIN,
                EtatCompte.ACTIF
        );

        try {
            // AJOUTER
            userService.ajouter(u1);

            // LIRE / AFFICHER tout
            System.out.println("\n--- Liste des Users ---");
            userService.lire(null); // null = lire tous les users

            // MODIFIER
            u1.setNom("Fatma Souei");
            userService.modifier(u1);

            System.out.println("\n--- Users après modification ---");
            userService.lire(null);

            // SUPPRIMER (optionnel)
            // userService.supprimer(u1.getUserId());

        } catch (SQLException e) {
            System.out.println("Erreur UserService : " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n========= TEST CRUD PROFIL =========");

        // Création Profil
        Profil profil = new Profil(
                "Bio Fatma",
                "22222222",
                "Fatma",
                "Souei",
                "image.png",
                u1 // association avec User
        );

        try {
            // AJOUTER
            profilService.ajouter(profil);

            // LIRE / AFFICHER tout
            System.out.println("\n--- Liste des Profils ---");
            profilService.lire(null); // null = lire tous les profils

            // MODIFIER
            Profil p2 = new Profil(
                    1, // ⚠️ ID existant en BD
                    "Nouvelle Bio",
                    "99999999",
                    "Fatma",
                    "Souei",
                    "new.png",
                    u1
            );
            profilService.modifier(p2);

            System.out.println("\n--- Profils après modification ---");
            profilService.lire(null);

            // SUPPRIMER (optionnel)
            // profilService.supprimer(p2.getId());

        } catch (SQLException e) {
            System.out.println("Erreur ProfilService : " + e.getMessage());
            e.printStackTrace();
        }

    }
}