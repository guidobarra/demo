package com.guba.service;

import com.guba.model.User;
import com.guba.repository.UserRepository;
import io.vertx.core.Future;

import java.util.List;
import java.util.Optional;

public class UserService {

  private final UserRepository repository;

  public UserService(UserRepository repository) {
    this.repository = repository;
  }

  public Future<List<User>> findAll() {
    return repository.findAll();
  }

  public Future<Optional<User>> findById(long id) {
    return repository.findById(id);
  }

  public Future<User> create(String name, String email) {
    User user = new User(name, email);
    return repository.save(user);
  }

  public Future<Optional<User>> update(long id, String name, String email) {
    User user = new User(name, email);
    return repository.update(id, user);
  }

  public Future<Boolean> delete(long id) {
    return repository.delete(id);
  }
}
