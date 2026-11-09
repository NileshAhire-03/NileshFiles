package com.nilspath.api;

import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/admin")
public class AdminManagementController {
    private final Companies companies;
    private final Jobs jobs;
    private final Settings settings;
    private final PlatformService platform;
    AdminManagementController(Companies companies, Jobs jobs, Settings settings, PlatformService platform) {
        this.companies=companies; this.jobs=jobs; this.settings=settings; this.platform=platform;
    }
    record CompanyInput(@NotBlank @Size(max=200) String name,@Size(max=100) String city,
        @Size(max=500) @Pattern(regexp="^$|https?://[^\\s]+",message="Enter an http:// or https:// website") String website) {}
    record SettingsInput(@NotNull Boolean registrationAllowed,@NotNull Boolean jobPostingAllowed) {}

    @GetMapping("/companies")
    @Transactional(readOnly=true)
    public List<Company> list() {
        return companies.findAll();
    }
    @PostMapping("/companies")
    public Company create(@Valid @RequestBody CompanyInput input) {
        if (companies.findByNameIgnoreCase(input.name().trim()).isPresent()) throw new IllegalArgumentException("Company already exists.");
        Company company=new Company(input.name().trim()); company.city=input.city(); company.website=input.website();
        return companies.save(company);
    }
    @PutMapping("/companies/{id}") @Transactional
    public ResponseEntity<?> update(@PathVariable Long id,@Valid @RequestBody CompanyInput input) {
        Company company=companies.findById(id).orElse(null);
        if (company==null) return ResponseEntity.notFound().build();
        String name=input.name().trim();
        if (companies.findByNameIgnoreCase(name).filter(c -> !id.equals(c.id)).isPresent()) throw new IllegalArgumentException("Company already exists.");
        String oldName=company.name;
        for (Job job : jobs.findAll()) if (oldName.equalsIgnoreCase(job.company)) { job.company=name; jobs.save(job); }
        company.name=name; company.city=input.city(); company.website=input.website();
        return ResponseEntity.ok(companies.save(company));
    }
    @DeleteMapping("/companies/{id}") @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Company company=companies.findById(id).orElse(null);
        if (company==null) return ResponseEntity.notFound().build();
        if (jobs.findAll().stream().anyMatch(j -> company.name.equalsIgnoreCase(j.company)))
            return ResponseEntity.status(409).body(Map.of("error","This company has job listings and cannot be deleted."));
        companies.delete(company);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/settings") public PlatformSettings settings() { return platform.currentSettings(); }
    @PutMapping("/settings")
    public PlatformSettings saveSettings(@Valid @RequestBody SettingsInput input) {
        PlatformSettings value=platform.currentSettings();
        value.registrationAllowed=input.registrationAllowed(); value.jobPostingAllowed=input.jobPostingAllowed();
        return settings.save(value);
    }
}
