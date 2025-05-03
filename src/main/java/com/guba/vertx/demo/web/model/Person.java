package com.guba.vertx.demo.web.model;

public class Person {
  private String name;
  private Integer dni;
  private Integer years;

  public Person() {
  }
  public Person(String name, Integer dni, Integer years) {
    this.name = name;
    this.dni = dni;
    this.years = years;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getDni() {
    return dni;
  }

  public void setDni(Integer dni) {
    this.dni = dni;
  }

  public Integer getYears() {
    return years;
  }

  public void setYears(Integer years) {
    this.years = years;
  }
}
