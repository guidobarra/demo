package com.guba.repository;

import com.guba.model.User;
import io.vertx.core.Future;
import io.vertx.mysqlclient.MySQLClient;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {

  private final Pool pool;

  public UserRepository(Pool pool) {
    this.pool = pool;
  }

  public Future<List<User>> findAll() {
    return pool.query("SELECT * FROM users ORDER BY id")
      .execute()
      .map(rows -> {
        List<User> users = new ArrayList<>();
        for (Row row : rows) {
          users.add(User.fromRow(row));
        }
        return users;
      });
  }

  public Future<Optional<User>> findById(long id) {
    return pool.preparedQuery("SELECT * FROM users WHERE id = ?")
      .execute(Tuple.of(id))
      .map(rows -> {
        if (rows.size() == 0) return Optional.empty();
        return Optional.of(User.fromRow(rows.iterator().next()));
      });
  }

  public Future<User> save(User user) {
    return pool.preparedQuery("INSERT INTO users (name, email) VALUES (?, ?)")
      .execute(Tuple.of(user.getName(), user.getEmail()))
      .compose(result -> {
        long newId = result.property(MySQLClient.LAST_INSERTED_ID);
        return findById(newId);
      })
      .map(Optional::orElseThrow);
  }

  public Future<Optional<User>> update(long id, User user) {
    return pool.preparedQuery("UPDATE users SET name = ?, email = ? WHERE id = ?")
      .execute(Tuple.of(user.getName(), user.getEmail(), id))
      .compose(result -> {
        if (result.rowCount() == 0) return Future.succeededFuture(Optional.<User>empty());
        return findById(id);
      });
  }

  public Future<Boolean> delete(long id) {
    return pool.preparedQuery("DELETE FROM users WHERE id = ?")
      .execute(Tuple.of(id))
      .map(result -> result.rowCount() > 0);
  }
}
