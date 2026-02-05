package entities;

public class User {

    private int id;
    private String nom;
    private String email;
    private String motDePasse;
    private Role role;
    private EtatCompte etatCompte;
    private Profil profil;

    // Constructeur vide
    public User() {
    }

    // Constructeur sans profil
    public User(int id, String nom, String email, String motDePasse,
                Role role, EtatCompte etatCompte) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.role = role;
        this.etatCompte = etatCompte;
    }

    // Constructeur complet
    public User(int id, String nom, String email, String motDePasse,
                Role role, EtatCompte etatCompte, Profil profil) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.role = role;
        this.etatCompte = etatCompte;
        this.profil = profil;
    }
    // Constructeur sans id
    public User(String nom, String email, String motDePasse, Role role, EtatCompte etatCompte) {
        this.nom = nom;
        this.email = email;
        this.motDePasse = motDePasse;
        this.role = role;
        this.etatCompte = etatCompte;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public EtatCompte getEtatCompte() {
        return etatCompte;
    }

    public void setEtatCompte(EtatCompte etatCompte) {
        this.etatCompte = etatCompte;
    }

    public Profil getProfil() {
        return profil;
    }

    public void setProfil(Profil profil) {
        this.profil = profil;
    }

    // toString
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", etatCompte=" + etatCompte +
                '}';
    }
}
