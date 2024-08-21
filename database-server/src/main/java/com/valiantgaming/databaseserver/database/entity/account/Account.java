package com.valiantgaming.databaseserver.database.entity.account;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@NamedStoredProcedureQueries
({
    @NamedStoredProcedureQuery
    (
        name = "CreateAccount",
        procedureName = "CreateAccount",
        resultClasses = String.class,
        parameters =
        {
            @StoredProcedureParameter
            (
                name = "username",
                type = String.class,
                mode = ParameterMode.IN
            ),
            @StoredProcedureParameter
            (
                name = "password",
                type = String.class,
                mode = ParameterMode.IN
            ),
            @StoredProcedureParameter
            (
                name = "salt",
                type = String.class,
                mode = ParameterMode.IN
            ),
            @StoredProcedureParameter
            (
                name = "email",
                type = String.class,
                mode = ParameterMode.IN
            ),
            @StoredProcedureParameter
            (
                name = "firstName",
                type = String.class,
                mode = ParameterMode.IN
            ),
            @StoredProcedureParameter
            (
                name = "lastName",
                type = String.class,
                mode = ParameterMode.IN
            ),
            @StoredProcedureParameter
            (
                name = "birthMonth",
                type = int.class,
                mode = ParameterMode.IN
            ),
            @StoredProcedureParameter
            (
                name = "birthDay",
                type = int.class,
                mode = ParameterMode.IN
            ),
            @StoredProcedureParameter
            (
                name = "securityQ1",
                type = String.class,
                mode = ParameterMode.IN
            ),
            @StoredProcedureParameter
            (
                name = "answer1",
                type = String.class,
                mode = ParameterMode.IN
            ),
            @StoredProcedureParameter
            (
                name = "securityQ2",
                type = String.class,
                mode = ParameterMode.IN
            ),
            @StoredProcedureParameter
            (
                name = "answer2",
                type = String.class,
                mode = ParameterMode.IN
            ),
            @StoredProcedureParameter
            (
                name = "securityQ3",
                type = String.class,
                mode = ParameterMode.IN
            ),
            @StoredProcedureParameter
            (
                name = "answer3",
                type = String.class,
                mode = ParameterMode.IN
            )
        }
    ),
    @NamedStoredProcedureQuery
    (
        name = "UpdateAllPasswords",
        procedureName = "UpdateAllPasswords",
        resultClasses = String.class
    ),
    @NamedStoredProcedureQuery
    (
        name = "UpdatePassword",
        procedureName = "UpdatePassword",
        resultClasses = String.class,
        parameters =
        {
            @StoredProcedureParameter
            (
                name = "user",
                type = Account.class,
                mode = ParameterMode.IN
            )
        }
    )
})
@Table @Entity
@Getter @Setter
@NoArgsConstructor
public class Account
{
    @Id
    @Column(name = "AccountID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int accountID;

    @Column(name = "AccountStorageID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int accountStorageID;

    @Column(name = "CharacterSlotID")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int characterSlotID;

    @Column(name = "ProfileID")
    @GeneratedValue(strategy = GenerationType.AUTO)
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

    @Column(name = "LockType")
    private String lockType;

    @Column(name = "LockUntil")
    private LocalDateTime lockUntil;

    @NotBlank
    @Column(name = "IsBanned")
    private boolean banned;

    @Column(name = "BanType")
    private String banType;

    @Column(name = "BannedUntil")
    private LocalDateTime bannedUntil;

    @Column(name = "LastBanned")
    private LocalDateTime lastBanned;

    @NotNull
    @Column(name = "IsInDeletion")
    private boolean inDeletion;

    @Column(name = "DeletionDate")
    private LocalDateTime deletionDate;

    @NotBlank
    @Column(name = "CreateDate")
    private LocalDateTime createDate;

    @Column(name = "ModifiedDate")
    private LocalDateTime modifiedDate;

    @Override
    public String toString() {
        return "Account{" +
                "accountID=" + accountID +
                ", accountStorageID=" + accountStorageID +
                ", characterSlotID=" + characterSlotID +
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
                ", inDeletion=" + inDeletion +
                ", deletionDate=" + deletionDate +
                ", createDate=" + createDate +
                ", modifiedDate=" + modifiedDate +
                '}';
    }
}