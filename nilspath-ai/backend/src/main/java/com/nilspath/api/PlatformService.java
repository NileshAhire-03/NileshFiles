package com.nilspath.api;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.dao.DataIntegrityViolationException;

@Service
public class PlatformService {
    private final Companies companies;
    private final Settings settings;
    PlatformService(Companies companies, Settings settings) { this.companies=companies; this.settings=settings; }
    public PlatformSettings currentSettings() { return settings.findById(1L).orElseGet(PlatformSettings::new); }
    public void requireRegistration() {
        if (!currentSettings().registrationAllowed) throw new IllegalArgumentException("New account registration is currently disabled by the administrator.");
    }
    public void requireJobPosting() {
        if (!currentSettings().jobPostingAllowed) throw new IllegalArgumentException("New job posting is currently disabled by the administrator.");
    }
    // Repository writes run in their own transaction. If another request wins
    // the unique-name insert, its committed company can be read after rollback.
    @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public Company ensureCompany(String name) {
        String normalized=name.trim();
        Company existing=companies.findByNameIgnoreCase(normalized).orElse(null);
        if(existing!=null)return existing;
        try {
            return companies.saveAndFlush(new Company(normalized));
        } catch(DataIntegrityViolationException conflict) {
            return companies.findByNameIgnoreCase(normalized).orElseThrow(() -> conflict);
        }
    }
}
