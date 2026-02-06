package main;

import entities.*;
import service.*;

public class Main {

    public static void main(String[] args) {

        UserService userService = new UserService();
        ProfilService profilService = new ProfilService();

        System.out.println("========= TEST CRUD USER =========");

        User u1 = new User(
                "Fatma",
                "fatma_test@gmail.com",
                "123456",
                Role.ADMIN,
                EtatCompte.ACTIF
        );

        // 🔥 OBLIGATOIRE : ajouter avant modifier
        userService.ajouter(u1);

        userService.afficher();

        // Modifier User
        u1.setNom("Fatma Souei");
        userService.modifier(u1);

        System.out.println("\n--- Users après modification ---");
        userService.afficher();

        System.out.println("\n========= TEST CRUD PROFIL =========");

        Profil profil = new Profil(
                "Bio Fatma",
                "22222222",
                "Fatma",
                "Souei",
                "image.png",
                u1
        );

        profilService.ajouter(profil);
        profilService.afficher();

        // Modifier Profil
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
        profilService.afficher();
    }
}
