package com.nilspath.api;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class Bootstrap {
    @Bean
    CommandLineRunner seed(Accounts accounts, Jobs jobs, PasswordEncoder encoder, PlatformService platform,
        @Value("${app.admin.username}") String username, @Value("${app.admin.password}") String password) {
        return args -> {
            if (!username.isBlank() && !password.isBlank() && password.length()>=12 && accounts.findByUsernameIgnoreCaseAndRole(username,"ADMIN").isEmpty())
                accounts.save(new Account(username,encoder.encode(password),"ADMIN",true,"Administrator"));
            if (jobs.count()==0) {
                jobs.save(new Job("Java Backend Developer","TechNova","Pune","Full Time","java,spring boot,sql",25,null));
                jobs.save(new Job("React Developer","PixelWorks","Mumbai","Remote","react,javascript,css",40,null));
                jobs.save(new Job("Software Engineer","CloudAxis","Hyderabad","Internship","java,git,sql",30,null));
                jobs.save(new Job("Full Stack Developer","BlueStack","Pune","Work from Home","react,java,html,css",50,null));
            }
            // Backfill seed/legacy companies at startup, never during GET requests.
            jobs.findAll().stream().map(job -> job.company)
                .filter(name -> name!=null && !name.isBlank()).distinct().forEach(platform::ensureCompany);
        };
    }
}
