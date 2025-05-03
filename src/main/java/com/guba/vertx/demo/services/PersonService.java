package com.guba.vertx.demo.services;

import com.guba.vertx.demo.web.model.Person;
import io.vertx.core.Future;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PersonService {

  private final List<Person> people = new ArrayList<>();

  public Future<List<Person>> all() {
    return Future.succeededFuture(people);
  }

  public Future<Person> create(Person person) {
    people.add(person);
    return Future.succeededFuture(person);
  }

  public Future<Person> findByDni(Integer dni) {
    return Future.<Person>future(promise -> {
      Optional<Person> personExist = people.stream()
        .filter(p -> p.getDni().equals(dni))
        .findFirst();

      if (personExist.isPresent()) {
        promise.complete(personExist.get());
      } else {
        promise.fail("Person dni " + dni + " not found");
      }
    });
  }

  public Future<Person> update(Integer dni, Person person) {
    return findByDni(dni)
      .map(p -> {
        p.setName(person.getName());
        p.setYears(person.getYears());
        return p;
      })
      .compose(Future::succeededFuture);
  }

  public Future<Void> delete(Integer dni) {
    return findByDni(dni).compose(person -> {
      people.remove(person);
      return Future.succeededFuture();
    });
  }
}
