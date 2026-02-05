package main;

import utils.DataBase;
import entities.User;
import entities.Role;
import entities.EtatCompte;
import service.UserService;
public class Main {
    public static void main(String[] args) {

        UserService us = new UserService();

        // 🔹 Ajouter
        User u1 = new User(
                "Fatma",
                "fatma@gmail.com",
                "123456",
                Role.AGRICULTEUR,
                EtatCompte.ACTIF
        );

        us.ajouter(u1);


        // 🔹 Afficher
        System.out.println("----- Liste Users -----");
        us.afficher();


        // 🔹 Modifier (exemple id = 1)
        User u2 = new User(
                1,
                "Fatma Souei",
                "fatma@gmail.com",
                "111111",
                Role.ADMIN,
                EtatCompte.ACTIF
        );

        us.modifier(u2);


        // 🔹 Supprimer
        // us.supprimer(1);

    }
}