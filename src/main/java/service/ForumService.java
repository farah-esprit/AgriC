package service;

import java.util.List;

public interface ForumService<T> {

    void add(T t);

    void update(T t);

    void delete(int id);

    List<T> getAll();
}
