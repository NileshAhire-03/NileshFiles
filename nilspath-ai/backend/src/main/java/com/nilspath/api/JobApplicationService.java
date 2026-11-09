package com.nilspath.api;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class JobApplicationService {
    private final Jobs jobs;
    private final Applications applications;

    JobApplicationService(Jobs jobs, Applications applications) {
        this.jobs=jobs;
        this.applications=applications;
    }

    @Transactional
    public ApplicationRecord apply(Long jobId, Long candidateId, ApiController.ApplicationInput input) {
        // Closing and applying acquire the same row lock. A job cannot close
        // between checking availability and saving the application.
        Job job=jobs.findLockedById(jobId).orElseThrow(() -> unavailable());
        if (!job.active) throw unavailable();
        if (applications.existsByJobIdAndCandidateId(jobId,candidateId))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"You have already applied for this job.");
        ApplicationRecord application=new ApplicationRecord(jobId,candidateId);
        if (input!=null) {
            application.objective=input.objective();
            application.previousWork=input.previousWork();
            application.expectedSalary=input.expectedSalary();
        }
        return applications.saveAndFlush(application);
    }

    @Transactional
    public Job close(Long jobId, Long ownerId) {
        Job job=jobs.findLockedById(jobId).orElseThrow(() -> unavailable());
        if (ownerId!=null && !ownerId.equals(job.hrId)) throw unavailable();
        job.active=false;
        return jobs.saveAndFlush(job);
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND,"Job is no longer available.");
    }
}
