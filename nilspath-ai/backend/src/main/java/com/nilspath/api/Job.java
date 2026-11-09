package com.nilspath.api;
import jakarta.persistence.*;
@Entity public class Job {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public String title,company,city,type,skills;
 public int distanceKm;
 public Long hrId;
 public boolean active=true;
 public Job(){}
 public Job(String t,String c,String city,String type,String skills,int d,Long h){title=t;company=c;this.city=city;this.type=type;this.skills=skills;distanceKm=d;hrId=h;}
}
