package com.nilspath.api;
import jakarta.persistence.*;
@Entity public class Interview {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long applicationId;
 public String scheduledAt,notes;
 public Interview(){}
 public Interview(Long a,String t,String n){applicationId=a;scheduledAt=t;notes=n;}
}
