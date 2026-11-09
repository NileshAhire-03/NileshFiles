package com.nilspath.api;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
@RestController @RequestMapping("/api") public class ApiController {
 private final AccountDirectory directory;private final Accounts accounts;private final Jobs jobs;private final Applications apps;private final Interviews interviews;private final PasswordEncoder encoder;private final ResumeMatcher matcher;private final InterviewService scheduling;private final PlatformService platform;private final JobApplicationService jobApplications;
 ApiController(AccountDirectory directory,Accounts a,Jobs j,Applications ap,Interviews i,PasswordEncoder e,ResumeMatcher m,InterviewService scheduling,PlatformService platform,JobApplicationService jobApplications){this.directory=directory;this.jobApplications=jobApplications;accounts=a;jobs=j;apps=ap;interviews=i;encoder=e;matcher=m;this.scheduling=scheduling;this.platform=platform;}
 record Register(@NotBlank @Size(max=100) @Pattern(regexp="[A-Za-z0-9_.@-]+",message="Use letters, digits, dots, underscores, @ or hyphens") String username,@NotBlank @Size(min=8,max=72) String password,@NotBlank @Size(max=200) String fullName,@NotBlank String role,@Size(max=72) String confirmPassword){}
 record JobInput(@NotBlank @Size(max=200) String title,@NotBlank @Size(max=200) String company,@NotBlank @Size(max=100) String city,@NotBlank @Size(max=100) String type,@Size(max=255) String skills,@Min(0) int distanceKm){}
 record ApplicationInput(@Size(max=255) String objective,@Size(max=255) String previousWork,@Size(max=255) String expectedSalary){}
 record ResumeInput(@NotBlank @Size(max=150000) String resume){}
 record StatusInput(@NotBlank @Pattern(regexp="(?i)APPLIED|SHORTLISTED|REJECTED",message="Choose APPLIED, SHORTLISTED or REJECTED; use scheduling to create an interview") String status){}
 record InterviewInput(@NotBlank @Size(max=100) String scheduledAt,@Size(max=255) String notes){}
 Account current(Authentication auth){return directory.current(auth);}
 Map<String,Object> publicAccount(Account a){return Map.of("id",a.id,"username",a.username,"role",a.role,"approved",a.approved,"fullName",a.fullName==null?"":a.fullName);}
 @PostMapping("/auth/register") public ResponseEntity<?> register(@Valid @RequestBody Register r){platform.requireRegistration();if(r.confirmPassword()!=null&&!r.password().equals(r.confirmPassword()))return ResponseEntity.badRequest().body(Map.of("error","Passwords do not match"));if(r.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)return ResponseEntity.badRequest().body(Map.of("error","Password is too long. Please use a shorter password."));String role=r.role().trim().toUpperCase(Locale.ROOT);if(!List.of("CANDIDATE","HR").contains(role))return ResponseEntity.badRequest().body(Map.of("error","Invalid role"));if(accounts.findByUsernameIgnoreCaseAndRole(r.username(),role).isPresent())return ResponseEntity.status(409).body(Map.of("error","An account with this username already exists in the "+(role.equals("HR")?"HR":"Candidate")+" workspace. Use its sign-in page."));Account a=accounts.save(new Account(r.username(),encoder.encode(r.password()),role,role.equals("CANDIDATE"),r.fullName().strip()));return ResponseEntity.ok(publicAccount(a));}
 @GetMapping("/auth/me") public Object me(Authentication auth){return publicAccount(current(auth));}
 @GetMapping("/jobs") public List<Job> list(){return jobs.findByActiveTrue();}
 @PostMapping("/candidate/jobs/{id}/apply") public ApplicationRecord apply(@PathVariable Long id,@Valid @RequestBody(required=false) ApplicationInput input,Authentication auth){return jobApplications.apply(id,current(auth).id,input);}
 @GetMapping("/candidate/applications") public List<Map<String,Object>> myApps(Authentication auth){return apps.findByCandidateId(current(auth).id).stream().map(this::applicationView).toList();}
 @PostMapping("/candidate/analyze") public Object analyze(@Valid @RequestBody ResumeInput input){return matcher.score(input.resume(),jobs.findByActiveTrue());}
 @GetMapping("/hr/jobs") public List<Job> hrJobs(Authentication auth){Long id=current(auth).id;return jobs.findAll().stream().filter(j->id.equals(j.hrId)).toList();}
 @PostMapping("/hr/jobs") public Job post(@Valid @RequestBody JobInput in,Authentication auth){platform.requireJobPosting();Company company=platform.ensureCompany(in.company());return jobs.save(new Job(in.title(),company.name,in.city(),in.type(),in.skills(),Math.max(0,in.distanceKm()),current(auth).id));}
 @PutMapping("/hr/jobs/{id}/close") public Job close(@PathVariable Long id,Authentication auth){return jobApplications.close(id,current(auth).id);}
 @GetMapping("/hr/applications") public List<Map<String,Object>> hrApps(Authentication auth){Long id=current(auth).id;return apps.findAll().stream().filter(a->jobs.findById(a.jobId).map(j->id.equals(j.hrId)).orElse(false)).map(a->applicationView(a)).toList();}
 Map<String,Object> applicationView(ApplicationRecord a){Map<String,Object> m=new LinkedHashMap<>();m.put("id",a.id);m.put("jobId",a.jobId);m.put("job",jobs.findById(a.jobId).map(j->j.title).orElse("Unknown"));m.put("candidate",accounts.findById(a.candidateId).map(c->c.fullName).orElse("Unknown"));m.put("status",a.status);m.put("interviews",interviews.findByApplicationId(a.id));m.put("objective",a.objective);m.put("previousWork",a.previousWork);m.put("expectedSalary",a.expectedSalary);return m;}
 @PutMapping("/hr/applications/{id}/status") public ResponseEntity<?> status(@PathVariable Long id,@Valid @RequestBody StatusInput input,Authentication auth){ApplicationRecord a=apps.findById(id).orElse(null);if(a==null||!jobs.findById(a.jobId).map(j->current(auth).id.equals(j.hrId)).orElse(false))return ResponseEntity.notFound().build();String s=input.status().toUpperCase(Locale.ROOT);if(!List.of("APPLIED","SHORTLISTED","REJECTED").contains(s))return ResponseEntity.badRequest().build();a.status=s;return ResponseEntity.ok(apps.save(a));}
 @PostMapping("/hr/applications/{id}/interviews") public ResponseEntity<?> interview(@PathVariable Long id,@Valid @RequestBody InterviewInput input,Authentication auth){ApplicationRecord a=apps.findById(id).orElse(null);if(a==null||!jobs.findById(a.jobId).map(j->current(auth).id.equals(j.hrId)).orElse(false))return ResponseEntity.notFound().build();return ResponseEntity.ok(scheduling.schedule(a,input.scheduledAt(),input.notes()));}
 @GetMapping("/admin/accounts") public List<Map<String,Object>> users(){return accounts.findAll().stream().map(this::publicAccount).toList();}
 @PutMapping("/admin/hr/{id}/approve") public ResponseEntity<?> approve(@PathVariable Long id){Account a=accounts.findById(id).orElse(null);if(a==null||!a.role.equals("HR"))return ResponseEntity.notFound().build();a.approved=true;accounts.save(a);return ResponseEntity.ok(publicAccount(a));}
 @GetMapping("/admin/jobs") public List<Job> allJobs(){return jobs.findAll();}
 @PutMapping("/admin/jobs/{id}/close") public Job adminClose(@PathVariable Long id){return jobApplications.close(id,null);}
 @GetMapping("/admin/reports") public Object reports(){return Map.of("candidates",accounts.findByRole("CANDIDATE").size(),"hrAccounts",accounts.findByRole("HR").size(),"pendingHR",accounts.findByRole("HR").stream().filter(a->!a.approved).count(),"jobs",jobs.count(),"applications",apps.count(),"interviews",interviews.count());}
}
