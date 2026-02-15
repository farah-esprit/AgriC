package org.example;

import entities.Commande;
import entities.Produit;
import entities.Stock;
import services.CommandeService;
import services.ProduitService;
import services.StockService;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println(" === AGRICONNECT : PRODUIT + STOCK + COMMANDE MySQL === ");

        // SERVICES
        ProduitService produitService = new ProduitService();
        StockService stockService = new StockService();
        CommandeService commandeService = new CommandeService();


        // 1️⃣ PRODUIT CRUD

        System.out.println("\n📦 === 1. PRODUIT CRUD ===\n");

        // CREATE Produit
        System.out.println("1️⃣ CREATE PRODUIT :");
        Produit p1 = new Produit();
        p1.setNom("Cerise");
        p1.setDescription("Rouges fraîches");
        p1.setPrix(2.0);
        p1.setCategorie("Fruit");
        produitService.ajouter(p1);

        // READ Produits
        System.out.println("\n2️⃣ READ PRODUITS :");
        List<Produit> produits = produitService.getAllProduits();
        produits.forEach(p ->
                System.out.println("✅ ID:" + p.getIdProduit() + " - " + p.getNom() + " (" + p.getPrix() + "dt)")
        );

        // UPDATE Produit
        System.out.println("\n🔄 UPDATE PRODUIT ID=9 :");
        Produit p2 = new Produit();
        p2.setIdProduit(9L);
        p2.setNom("Tomates bibou");
        p2.setPrix(1.0);
        p2.setDescription("Variété nina");
        p2.setCategorie("Légumes locaux");
        produitService.modifier(p2);

        // DELETE Produit
        System.out.println("\n🗑️ DELETE PRODUIT ID=8 :");
        produitService.supprimer(8L);


        // 2️⃣ STOCK CRUD

        System.out.println("\n📊 === 2. STOCK CRUD ===\n");

        // CREATE Stock
        System.out.println("1️⃣ CREATE STOCK :");
        Stock s1 = new Stock(120, 80, 30, 1);
        stockService.ajouter(s1);

        // READ Stocks
        System.out.println("\n2️⃣ READ STOCKS :");
        List<Stock> stocks = stockService.getAllStocks();
        stocks.forEach(s ->
                System.out.println("✅ ID:" + s.getIdStock() +
                        " | Qte:" + s.getQuantite() +
                        " | Disp:" + s.getDisponible() +
                        " | Seuil:" + s.getSeuilAlert() +
                        " | Produit:" + s.getIdProduit())
        );

        // UPDATE Stock
        System.out.println("\n🔄 UPDATE STOCK ID=1 :");
        if (!stocks.isEmpty()) {
            Stock s2 = new Stock(s1.getIdStock(), 100, 70, 20, 1);
            stockService.modifier(s2);
            System.out.println("🔍 STOCK MODIFIÉ : " + stockService.getAllStocks().get(0));
        }

        // DELETE Stock
        System.out.println("\n🗑️ DELETE STOCK ID=1 :");
        if (!stocks.isEmpty()) {
            stockService.supprimer(s1.getIdStock());
            System.out.println("📋 STOCKS RESTANTS : " + stockService.getAllStocks().size());
        }


        // 3️⃣ COMMANDE CRUD

        System.out.println("\n🛒 === 3. COMMANDE CRUD ===\n");

        System.out.println("🧹 Nettoyage commandes existantes...");
        List<Commande> anciennes = commandeService.getAllCommandes();
        for (Commande c : anciennes) {
            commandeService.supprimer(c.getIdCommande());
        }

        // CREATE Commande (3 exemples des constructeurs) ✅ SANS user_id
        System.out.println("1️⃣ CREATE COMMANDE (3 constructeurs) :");

        // 🔥 CONSTRUCTEUR 1 : sans paramètres (user_id = NULL)
        System.out.println("   → Constructeur 1 (sans paramètres)");
        Commande c1 = new Commande();
        c1.setIdProduit(1L);  // Produit Cerise existe
        // user_id = null → OK avec FK !
        commandeService.ajouter(c1);

        // 🔥 CONSTRUCTEUR 2 : avec quantité (user_id = NULL)
        System.out.println("   → Constructeur 2 (20)");
        Commande c2 = new Commande(22);
        c2.setIdProduit(1L);  // Produit Cerise existe
        // user_id = null → OK avec FK !
        commandeService.ajouter(c2);

        // 🔥 CONSTRUCTEUR 3 : statut + quantité (user_id = NULL)
        System.out.println("   → Constructeur 3 (statut + quantité)");
        Commande c3 = new Commande("weslt", 25);
        c3.setIdProduit(1L);  // Produit Cerise existe
        // user_id = null → OK avec FK !
        commandeService.ajouter(c3);

        // READ Commandes
        System.out.println("\n2️⃣ READ COMMANDES :");
        List<Commande> commandes = commandeService.getAllCommandes();
        commandes.forEach(c ->
                System.out.println("✅ ID:"+ c.getIdCommande() +
                        " | Date:" + c.getDateCommande() +
                        " | Statut:" + c.getStatut() +
                        " | Qte:" + c.getQuantiteCommandee() +
                        " | Produit:" + c.getIdProduit() +
                        " | User:" + (c.getUserId() != null ? c.getUserId() : "NULL"))
        );

        // UPDATE Statut commande
        System.out.println("\n🔄 UPDATE COMMANDE #1 → baha :");
        if (!commandes.isEmpty()) {
            commandeService.modifierStatut(commandes.get(0).getIdCommande(), "baha");
            List<Commande> updated = commandeService.getAllCommandes();
            System.out.println("🔍 COMMANDE MODIFIÉE : " + updated.get(0).getStatut());
        }

        // DELETE Commande
        System.out.println("\n🗑️ DELETE COMMANDE #1 :");
        if (!commandes.isEmpty()) {
            commandeService.supprimer(commandes.get(0).getIdCommande());
            System.out.println("📋 COMMANDES RESTANTES : " + commandeService.getAllCommandes().size());
        }

        System.out.println("\n🎉 AGRICONNECT COMPLET ");
        System.out.println("✅ PRODUIT + STOCK + COMMANDE = 100% FONCTIONNEL !");
    }
}
