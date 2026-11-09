package com.nilspath.api;
import jakarta.persistence.*;
@Entity @Table(name="accounts",uniqueConstraints=@UniqueConstraint(name="uk_accounts_username_role",columnNames={"username","role"})) public class Account {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false) public String username;
 @Column(nullable=false) public String password;
 @Column(nullable=false) public String role;
 public boolean approved;
 public String fullName;
 public Account(){}
 public Account(String u,String p,String r,boolean a,String n){username=u;password=p;role=r;approved=a;fullName=n;}
}
