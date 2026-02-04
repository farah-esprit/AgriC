package service;

public interface IService<T> {

    void ajouter(T t);

    void modifier(T t);

    void supprimer(int id);

    void afficher();
}

