package entities;

public class Culture {

    private int idCulture; // Auto-incrémenté par la base de données
    private String nom;
    private String type;
    private double superficie;
    private String localisation;
    private String image; // chemin image

    public Culture() {}

    // Constructeur SANS idCulture (pour l'insertion)
    public Culture(String nom, String type, double superficie, String localisation, String image) {
        this.nom = nom;
        this.type = type;
        this.superficie = superficie;
        this.localisation = localisation;
        this.image = image;
    }

    // Constructeur AVEC idCulture (pour la récupération depuis la BD)
    public Culture(int idCulture, String nom, String type, double superficie, String localisation, String image) {
        this.idCulture = idCulture;
        this.nom = nom;
        this.type = type;
        this.superficie = superficie;
        this.localisation = localisation;
        this.image = image;
    }

    // Validation simple (sans vérifier l'ID pour les nouvelles entrées)
    public boolean estValide() {
        return nom != null && !nom.trim().isEmpty() &&
                superficie > 0 && superficie <= 10000;
    }

    public String obtenirErreursValidation() {
        StringBuilder erreurs = new StringBuilder();
        if (nom == null || nom.trim().isEmpty()) erreurs.append("• Le nom est obligatoire\n");
        if (superficie <= 0 || superficie > 10000) erreurs.append("• La superficie doit être entre 0 et 10 000\n");
        return erreurs.toString();
    }

    // Getters / Setters
    public int getIdCulture() { return idCulture; }
    public void setIdCulture(int idCulture) { this.idCulture = idCulture; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getSuperficie() { return superficie; }
    public void setSuperficie(double superficie) { this.superficie = superficie; }

    public String getLocalisation() { return localisation; }
    public void setLocalisation(String localisation) { this.localisation = localisation; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    @Override
    public String toString() {
        return nom;
    }
}