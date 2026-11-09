package com.nilspath.api;

import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AccountDirectory {
    private final Accounts accounts;
    AccountDirectory(Accounts accounts) {this.accounts=accounts;}

    public String loginName(Account account) {return account.role+"/"+account.username;}

    public Account forLogin(String login) {
        int separator=login.indexOf('/');
        if(separator>=0) {
            String role=login.substring(0,separator).toUpperCase(Locale.ROOT);
            String username=login.substring(separator+1);
            if(!List.of("CANDIDATE","HR","ADMIN").contains(role)||username.isBlank())throw unknown();
            return accounts.findByUsernameIgnoreCaseAndRole(username,role).orElseThrow(this::unknown);
        }
        // Every request must select a workspace, even for a currently unique name.
        throw unknown();
    }

    public Account current(Authentication authentication) {return forLogin(authentication.getName());}

    private UsernameNotFoundException unknown() {return new UsernameNotFoundException("Unknown account or role-qualified username required.");}
}
