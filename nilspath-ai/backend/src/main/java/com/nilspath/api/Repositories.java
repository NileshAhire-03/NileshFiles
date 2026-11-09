package com.nilspath.api;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
interface Accounts extends JpaRepository<Account,Long>{Optional<Account> findByUsernameAndRole(String username,String role);Optional<Account> findByUsernameIgnoreCaseAndRole(String username,String role);List<Account> findAllByUsername(String username);List<Account> findAllByUsernameIgnoreCase(String username);List<Account> findByRole(String role);}
interface Jobs extends JpaRepository<Job,Long>{
 List<Job> findByActiveTrue();
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select j from Job j where j.id = :id")
 Optional<Job> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
}
interface Applications extends JpaRepository<ApplicationRecord,Long>{List<ApplicationRecord> findByCandidateId(Long id);List<ApplicationRecord> findByJobId(Long id);boolean existsByJobIdAndCandidateId(Long jobId,Long candidateId);}
interface Interviews extends JpaRepository<Interview,Long>{List<Interview> findByApplicationId(Long applicationId);}
interface Companies extends JpaRepository<Company,Long>{Optional<Company> findByNameIgnoreCase(String name);}
interface Settings extends JpaRepository<PlatformSettings,Long>{}
