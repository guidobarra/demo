package com.guba.model;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;

import java.time.LocalDateTime;

public class User {

  private Long id;
  private String name;
  private String email;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public User() {}

  public User(String name, String email) {
    this.name = name;
    this.email = email;
  }

  public static User fromRow(Row row) {
    User user = new User();
    user.setId(row.getLong("id"));
    user.setName(row.getString("name"));
    user.setEmail(row.getString("email"));
    user.setCreatedAt(row.getLocalDateTime("created_at"));
    user.setUpdatedAt(row.getLocalDateTime("updated_at"));
    return user;
  }

  public static User fromJson(JsonObject json) {
    User user = new User();
    user.setId(json.getLong("id"));
    user.setName(json.getString("name"));
    user.setEmail(json.getString("email"));
    return user;
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject()
      .put("name", name)
      .put("email", email);
    if (id != null) json.put("id", id);
    if (createdAt != null) json.put("createdAt", createdAt.toString());
    if (updatedAt != null) json.put("updatedAt", updatedAt.toString());
    return json;
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
