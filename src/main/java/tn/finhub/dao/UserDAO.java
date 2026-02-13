package tn.finhub.dao;

import tn.finhub.model.User;
import java.util.List;

public interface UserDAO {

    User findByEmail(String email);
    List<User> findAll();
    void delete(int id);
    void deleteAll();
    void insert(User user);

}
