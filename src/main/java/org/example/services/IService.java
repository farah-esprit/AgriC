package org.example.services;

import java.sql.SQLException;

public interface IService<T> {
    void ajouter(T t) throws SQLException;
    void supprimer(T t) throws SQLException;
    void modifier(T t) throws SQLException;
    void lire(T t) throws SQLException;

}
