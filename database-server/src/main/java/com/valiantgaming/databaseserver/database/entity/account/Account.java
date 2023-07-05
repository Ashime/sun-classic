package com.valiantgaming.databaseserver.database.entity.account;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Table @Entity
@Getter @Setter
@NoArgsConstructor
public class Account
{
    @Id
    @Column(name = "AccountID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int accountID;

    @NotBlank
    @Column(name = "ProfileID")
    private int profileID;

    @NotBlank
    @Column(name = "Username")
    private String username;

    @NotBlank
    @Column(name = "Password")
    private String password;

    @Column(name = "LastPassword")
    private String lastPassword;

    @NotBlank
    @Column(name = "Salt")
    private String salt;

    @Column(name = "LastSalt")
    private String lastSalt;

    @NotBlank
    @Column(name = "Email")
    private String email;

    @NotBlank
    @Column(name = "IsEmailVerified")
    private boolean emailVerified;

    @NotBlank
    @Column(name = "LoginAttempts")
    private int loginAttempts;

    @NotBlank
    @Column(name = "IsLocked")
    private boolean locked;

    @NotBlank
    @Column(name = "LockType")
    private String lockType;

    @Column(name = "LockUntil")
    private LocalDateTime lockUntil;

    @NotBlank
    @Column(name = "IsBanned")
    private boolean banned;

    @NotBlank
    @Column(name = "BanType")
    private String banType;

    @Column(name = "BannedUntil")
    private LocalDateTime bannedUntil;

    @Column(name = "LastBanned")
    private LocalDateTime lastBanned;

    @NotBlank
    @Column(name = "CreateDate")
    private LocalDateTime createDate;

    @Column(name = "ModifiedDate")
    private LocalDateTime modifiedDate;

    @Override
    public String toString()
    {
        return "Account{" +
                "accountID=" + accountID +
                ", profileID=" + profileID +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", lastPassword='" + lastPassword + '\'' +
                ", salt='" + salt + '\'' +
                ", lastSalt='" + lastSalt + '\'' +
                ", email='" + email + '\'' +
                ", emailVerified=" + emailVerified +
                ", loginAttempts=" + loginAttempts +
                ", locked=" + locked +
                ", lockType='" + lockType + '\'' +
                ", lockUntil=" + lockUntil +
                ", banned=" + banned +
                ", banType='" + banType + '\'' +
                ", bannedUntil=" + bannedUntil +
                ", lastBanned=" + lastBanned +
                ", createDate=" + createDate +
                ", modifiedDate=" + modifiedDate +
                '}';
    }
}