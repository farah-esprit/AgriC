package entities;

import java.time.LocalDateTime;

public class Commande {
    private int idCommande;
    private LocalDateTime dateCommande;
    private String statut;
    private int quantiteCommandee;
    private Long idProduit;
    private Integer userId;

    // 1️⃣ CONSTRUCTEUR SANS PARAMÈTRES (par défaut)
    public Commande() {
        this.dateCommande = LocalDateTime.now();
        this.statut = "EN_COURS";
        this.quantiteCommandee = 0;
    }

    // 2️⃣ CONSTRUCTEUR AVEC 1 PARAMÈTRE (quantité)
    public Commande(int quantiteCommandee) {
        this();  // appelle le constructeur sans paramètres
        this.quantiteCommandee = quantiteCommandee;
    }

    // 3️⃣ CONSTRUCTEUR SANS ID (statut + quantité)
    public Commande(String statut, int quantiteCommandee) {
        this.dateCommande = LocalDateTime.now();
        this.statut = statut;
        this.quantiteCommandee = quantiteCommandee;
    }

    // Getters/Setters
    public int getIdCommande() { return idCommande; }
    public void setIdCommande(int idCommande) { this.idCommande = idCommande; }

    public LocalDateTime getDateCommande() { return dateCommande; }
    public void setDateCommande(LocalDateTime dateCommande) { this.dateCommande = dateCommande; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public int getQuantiteCommandee() { return quantiteCommandee; }
    public void setQuantiteCommandee(int quantiteCommandee) { this.quantiteCommandee = quantiteCommandee; }

    public Long getIdProduit() { return idProduit; }
    public void setIdProduit(Long idProduit) { this.idProduit = idProduit; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    @Override
    public String toString() {
        return "Commande{" +
                "id=" + idCommande +
                ", date=" + dateCommande +
                ", statut='" + statut + '\'' +
                ", qte=" + quantiteCommandee +
                ", produit=" + idProduit +
                ", user=" + userId +
                '}';
    }
}
