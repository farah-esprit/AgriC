package entities;

public class Produit {
    private long idProduit;
    private String nom;
    private String description;
    private double prix;
    private String categorie;
    private boolean actif = true;
    private String imagePath;
    private boolean promo = false;

    public Produit() {}

    public Produit(long idProduit, String nom, String description, double prix, String categorie, boolean actif, String imagePath) {
        this.idProduit = idProduit;
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.categorie = categorie;
        this.actif = actif;
        this.imagePath = imagePath;
    }

    public Produit(long idProduit, String nom, String description, double prix, String categorie, boolean actif, String imagePath, boolean promo) {
        this.idProduit = idProduit;
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.categorie = categorie;
        this.actif = actif;
        this.imagePath = imagePath;
        this.promo = promo; // ✅
    }

    public Produit(String nom, String description, double prix, String categorie, String imagePath) {
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.categorie = categorie;
        this.actif = true;
        this.imagePath = imagePath;
    }

    public Produit(String nom, String description, double prix, String categorie, String imagePath, boolean promo) {
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.categorie = categorie;
        this.actif = true;
        this.imagePath = imagePath;
        this.promo = promo; // ✅
    }

    public Produit(long idProduit, String nom, String description, double prix, String categorie, boolean actif) {
        this.idProduit = idProduit;
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.categorie = categorie;
        this.actif = actif;
        this.imagePath = null;
    }

    public Produit(String nom, String description, double prix, String categorie) {
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.categorie = categorie;
        this.actif = true;
        this.imagePath = null;
    }

    // GETTERS & SETTERS
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
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public boolean isPromo() { return promo; } // ✅
    public void setPromo(boolean promo) { this.promo = promo; } // ✅

    @Override
    public String toString() {
        return "Produit{id=" + idProduit + ", nom='" + nom + "', prix=" + prix + ", promo=" + promo + '}';
    }
}