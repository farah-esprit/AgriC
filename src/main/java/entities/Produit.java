package entities;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class Produit {
    private long idProduit;
    private String nom;
    private String description;
    private double prix;
    private String categorie;
    private boolean actif=true;

    // 1️⃣ Constructeur SANS PARAMÈTRES
    public Produit() {}

    // 2️⃣ Constructeur AVEC TOUS PARAMÈTRES (id inclus)
    public Produit(long idProduit, String nom, String description, double prix, String categorie, boolean actif) {
        this.idProduit = idProduit;
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.categorie = categorie;
        this.actif = actif;
    }

    // 3️⃣ Constructeur SANS ID (pour création)
    public Produit(String nom, String description, double prix, String categorie) {
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.categorie = categorie;
        this.actif = true; // par défaut
    }

    // Getters/Setters (comme avant)
    public long getIdProduit() { return idProduit; }
    public void setIdProduit(long idProduit) { this.idProduit = idProduit; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrix() { return prix; }
    public void setPrix(double prix) { this.prix = prix; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    @Override
    public String toString() {
        return "Produit{id=" + idProduit + ", nom='" + nom + "', prix=" + prix + ", cat=" + categorie + "}";
    }
}
