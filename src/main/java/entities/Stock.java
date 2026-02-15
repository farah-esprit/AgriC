package entities;

public class Stock {
    private int idStock;
    private int quantite;
    private int disponible;
    private int seuilAlert;
    private int idProduit;

    // 1️⃣ CONSTRUCTEUR SANS PARAMÈTRES (par défaut)
    public Stock() {}

    // 2️⃣ CONSTRUCTEUR SANS ID (les 4 autres champs)
    public Stock(int quantite, int disponible, int seuilAlert, int idProduit) {
        this.quantite = quantite;
        this.disponible = disponible;
        this.seuilAlert = seuilAlert;
        this.idProduit = idProduit;
    }

    // 3️⃣ CONSTRUCTEUR AVEC TOUS LES PARAMÈTRES
    public Stock(int idStock, int quantite, int disponible, int seuilAlert, int idProduit) {
        this.idStock = idStock;
        this.quantite = quantite;
        this.disponible = disponible;
        this.seuilAlert = seuilAlert;
        this.idProduit = idProduit;
    }

    // Getters/Setters
    public int getIdStock() { return idStock; }
    public void setIdStock(int idStock) { this.idStock = idStock; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public int getDisponible() { return disponible; }
    public void setDisponible(int disponible) { this.disponible = disponible; }

    public int getSeuilAlert() { return seuilAlert; }
    public void setSeuilAlert(int seuilAlert) { this.seuilAlert = seuilAlert; }

    public int getIdProduit() { return idProduit; }
    public void setIdProduit(int idProduit) { this.idProduit = idProduit; }

    // toString() pour affichage
    @Override
    public String toString() {
        return "Stock{" +
                "idStock=" + idStock +
                ", quantite=" + quantite +
                ", disponible=" + disponible +
                ", seuilAlert=" + seuilAlert +
                ", idProduit=" + idProduit +
                '}';
    }
}
