package entities;

public class Produit {
    private long idProduit;
    private String nom;
    private String description;
    private double prix;
    private String categorie;
    private boolean actif = true;
    private String imagePath;  // ✅ NOUVEAU CHAMP

    // ═══════════════════════════════════════════════════════════
    // 1️⃣ Constructeur SANS PARAMÈTRES
    // ═══════════════════════════════════════════════════════════
    public Produit() {}

    // ═══════════════════════════════════════════════════════════
    // 2️⃣ Constructeur AVEC TOUS PARAMÈTRES (avec ID) - Pour modification
    // ═══════════════════════════════════════════════════════════
    public Produit(long idProduit, String nom, String description, double prix, String categorie, boolean actif, String imagePath) {
        this.idProduit = idProduit;
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.categorie = categorie;
        this.actif = actif;
        this.imagePath = imagePath;
    }

    // ═══════════════════════════════════════════════════════════
    // 3️⃣ Constructeur SANS ID (pour création/ajout)
    // ═══════════════════════════════════════════════════════════
    public Produit(String nom, String description, double prix, String categorie, String imagePath) {
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.categorie = categorie;
        this.actif = true; // Par défaut
        this.imagePath = imagePath;
    }

    // ═══════════════════════════════════════════════════════════
    // 4️⃣ Constructeur ANCIEN (pour compatibilité) - SANS imagePath
    // ═══════════════════════════════════════════════════════════
    public Produit(long idProduit, String nom, String description, double prix, String categorie, boolean actif) {
        this.idProduit = idProduit;
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.categorie = categorie;
        this.actif = actif;
        this.imagePath = null;
    }

    // ═══════════════════════════════════════════════════════════
    // 5️⃣ Constructeur ANCIEN (pour compatibilité) - SANS imagePath
    // ═══════════════════════════════════════════════════════════
    public Produit(String nom, String description, double prix, String categorie) {
        this.nom = nom;
        this.description = description;
        this.prix = prix;
        this.categorie = categorie;
        this.actif = true;
        this.imagePath = null;
    }

    // ═══════════════════════════════════════════════════════════
    // GETTERS & SETTERS
    // ═══════════════════════════════════════════════════════════

    public long getIdProduit() {
        return idProduit;
    }

    public void setIdProduit(long idProduit) {
        this.idProduit = idProduit;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrix() {
        return prix;
    }

    public void setPrix(double prix) {
        this.prix = prix;
    }

    public String getCategorie() {
        return categorie;
    }

    public void setCategorie(String categorie) {
        this.categorie = categorie;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    // ✅ NOUVEAU GETTER/SETTER pour imagePath
    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    // ═══════════════════════════════════════════════════════════
    // MÉTHODE toString()
    // ═══════════════════════════════════════════════════════════
    @Override
    public String toString() {
        return "Produit{" +
                "id=" + idProduit +
                ", nom='" + nom + '\'' +
                ", description='" + description + '\'' +
                ", prix=" + prix +
                ", categorie='" + categorie + '\'' +
                ", actif=" + actif +
                ", imagePath='" + imagePath + '\'' +
                '}';
    }
}