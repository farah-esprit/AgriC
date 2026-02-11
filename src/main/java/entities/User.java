package entities;

import java.time.LocalDateTime;

public class User {

    private int id;
    private String nom;
    private String email;
    private String motDePasse;
    private Role role;
    private EtatCompte etatCompte;
    private LocalDateTime dateCreation;

    // ================= CONSTRUCTEURS =================

    public User() {}

    // Constructeur sans ID (pour insertion)
    public User(String nom, String email, String motDePasse, Role role, EtatCompte etatCompte) {
        this.nom = nom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.role = role;
        this.etatCompte = etatCompte;
        this.dateCreation = LocalDateTime.now();
    }

    // Constructeur avec ID (pour lecture depuis BD)
    public User(int id, String nom, String email, String motDePasse, Role role, EtatCompte etatCompte) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.role = role;
        this.etatCompte = etatCompte;
    }

    // Constructeur complet avec date
    public User(int id, String nom, String email, String motDePasse, Role role, EtatCompte etatCompte, LocalDateTime dateCreation) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.role = role;
        this.etatCompte = etatCompte;
        this.dateCreation = dateCreation;
    }

    // ================= GETTERS =================

    public int getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getEmail() {
        return email;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public Role getRole() {
        return role;
    }

    public EtatCompte getEtatCompte() {
        return etatCompte;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    // ================= SETTERS =================

    public void setId(int id) {
        this.id = id;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public void setEtatCompte(EtatCompte etatCompte) {
        this.etatCompte = etatCompte;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    // ================= MÉTHODES UTILITAIRES =================

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", etatCompte=" + etatCompte +
                ", dateCreation=" + dateCreation +
                '}';
    }
}