package com.nilspath.api;

import jakarta.persistence.*;

@Entity @Table(name="companies")
public class Company {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
    @Column(nullable=false,unique=true,length=200) public String name;
    @Column(length=100) public String city;
    @Column(length=500) public String website;
    public Company() {}
    Company(String name) { this.name=name; }
}
