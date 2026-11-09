package com.nilspath.api;

import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/auth")
public class LoginController {
    private final Accounts accounts;
    private final PasswordEncoder encoder;
    private final String missingAccountHash;

    LoginController(Accounts accounts, PasswordEncoder encoder) {
        this.accounts=accounts;this.encoder=encoder;
        missingAccountHash=encoder.encode(UUID.randomUUID().toString());
    }

    record LoginInput(@NotBlank @Size(max=255) String username,
        @NotBlank @Size(max=72) String password,
        @NotBlank @Pattern(regexp="(?i)CANDIDATE|HR|ADMIN") String role) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginInput input) {
        if(input.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)
            return ResponseEntity.badRequest().body(Map.of("error","Password is too long. Please use a shorter password."));
        String role=input.role().toUpperCase(Locale.ROOT);
        Account account=accounts.findByUsernameIgnoreCaseAndRole(input.username().trim(),role).orElse(null);
        // Verify credentials before disclosing approval state. Unknown accounts
        // still perform a bcrypt comparison and receive the same generic error.
        boolean valid=encoder.matches(input.password(),account==null?missingAccountHash:account.password);
        if(account==null||!valid)
            return ResponseEntity.status(401).body(Map.of("code","INVALID_CREDENTIALS","error","Incorrect username or password for the selected workspace."));
        if(!account.approved) {
            String message=role.equals("HR")?"Your HR registration is awaiting Admin approval. Your Candidate account, if registered separately, is unaffected.":"This account is not approved for sign-in.";
            return ResponseEntity.status(403).body(Map.of("code",role.equals("HR")?"HR_APPROVAL_PENDING":"ACCOUNT_NOT_APPROVED","error",message));
        }
        return ResponseEntity.ok(Map.of("id",account.id,"username",account.username,"role",account.role,
            "approved",account.approved,"fullName",Objects.toString(account.fullName,"")));
    }
}
