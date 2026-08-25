package com.lose10.profile;

import com.lose10.platform.persistence.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "user_profiles")
public class UserProfile extends MutableEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column
    private String sex;

    @Column(name = "height_cm")
    private java.math.BigDecimal heightCm;

    @Column(length = 2)
    private String country;

    @Column(nullable = false, length = 64)
    private String timezone = "UTC";

    @Column(nullable = false, length = 10)
    private String locale = "en";

    protected UserProfile() {
    }

    public UserProfile(UUID userId, String timezone, String locale) {
        this.userId = userId;
        this.timezone = timezone;
        this.locale = locale;
    }

    public void update(String displayName, LocalDate dateOfBirth, String sex, java.math.BigDecimal heightCm, String country, String timezone) {
        this.displayName = displayName;
        this.dateOfBirth = dateOfBirth;
        this.sex = sex;
        this.heightCm = heightCm;
        this.country = country;
        if (timezone != null) {
            this.timezone = timezone;
        }
    }

    public UUID getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getSex() {
        return sex;
    }

    public java.math.BigDecimal getHeightCm() {
        return heightCm;
    }

    public String getCountry() {
        return country;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getLocale() {
        return locale;
    }
}
