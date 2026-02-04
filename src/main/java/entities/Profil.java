package entities;

public class Profil {

    private int id;
    private String bio;
    private String telephone;
    private User user;

    // Constructeur vide
    public Profil() {
    }

    // Constructeur avec paramètres
    public Profil(int id, String bio, String telephone, User user) {
        this.id = id;
        this.bio = bio;
        this.telephone = telephone;
        this.user = user;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // toString
    @Override
    public String toString() {
        return "Profil{" +
                "id=" + id +
                ", bio='" + bio + '\'' +
                ", telephone='" + telephone + '\'' +
                '}';
    }
}
