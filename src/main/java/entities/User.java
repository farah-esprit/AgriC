package entities;

public class User {

    private int id;
    private String nom;
    private String email;
    private String motDePasse;
    private Role role;
    private EtatCompte etatCompte;

    public User() {}

    public User(String nom, String email, String motDePasse, Role role, EtatCompte etatCompte) {
        this.nom = nom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.role = role;
        this.etatCompte = etatCompte;
    }

    public User(int id, String nom, String email, String motDePasse, Role role, EtatCompte etatCompte) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.role = role;
        this.etatCompte = etatCompte;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
}
