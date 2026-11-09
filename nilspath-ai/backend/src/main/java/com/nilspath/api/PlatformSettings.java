package com.nilspath.api;

import jakarta.persistence.*;

@Entity @Table(name="platform_settings")
public class PlatformSettings {
    @Id public Long id=1L;
    public boolean registrationAllowed=true;
    public boolean jobPostingAllowed=true;
    public PlatformSettings() {}
}
