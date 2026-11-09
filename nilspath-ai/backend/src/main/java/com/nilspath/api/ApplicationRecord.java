package com.nilspath.api;
import jakarta.persistence.*;
@Entity @Table(name="job_applications",uniqueConstraints=@UniqueConstraint(columnNames={"job_id","candidate_id"})) public class ApplicationRecord {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(name="job_id") public Long jobId;
 @Column(name="candidate_id") public Long candidateId;
 public String status="APPLIED";
 public String objective,previousWork,expectedSalary;
 public ApplicationRecord(){}
 public ApplicationRecord(Long j,Long c){jobId=j;candidateId=c;}
}
