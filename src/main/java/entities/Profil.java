package entities;

public class Profil {

    private int id;
    private String bio;
    private String telephone;
    private String nom;
    private String prenom;
    private String image;
    private User user;

    public Profil() {}

    public Profil(String bio, String telephone, String nom, String prenom, String image, User user) {
        this.bio = bio;
        this.telephone = telephone;
        this.nom = nom;
        this.prenom = prenom;
        this.image = image;
        this.user = user;
    }

    public Profil(int id, String bio, String telephone, String nom, String prenom, String image, User user) {
        this.id = id;
        this.bio = bio;
        this.telephone = telephone;
        this.nom = nom;
        this.prenom = prenom;
        this.image = image;
        this.user = user;
    }

    public int getId() {
        return id;
    }

    public String getBio() {
        return bio;
    }

    public String getTelephone() {
        return telephone;
    }

    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getImage() {
        return image;
    }

    public User getUser() {
        return user;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
