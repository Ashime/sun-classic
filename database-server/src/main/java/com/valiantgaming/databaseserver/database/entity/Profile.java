package com.valiantgaming.databaseserver.database.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Table @Entity
@Getter @Setter
@NoArgsConstructor
public class Profile
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ProfileID")
    private int profileID;

    @Column(name = "FirstName")
    private String firstName;

    @NotBlank
    @Column(name = "LastName")
    private String lastName;

    @Column(name = "BirthMonth")
    private short birthMonth;

    @Column(name = "BirthDay")
    private short birthDay;

    @Column(name = "SecurityQuestion1")
    private String securityQuestion1;

    @Column(name = "Answer1")
    private String answer1;

    @Column(name = "SecurityQuestion2")
    private String securityQuestion2;

    @Column(name = "Answer2")
    private String answer2;

    @Column(name = "SecurityQuestion3")
    private String securityQuestion3;

    @Column(name = "Answer3")
    private String answer3;

    @NotNull
    @Column(name = "CreateDate")
    private LocalDateTime createDate;

    @Column(name = "ModifiedDate")
    private LocalDateTime modifiedDate;

    @Override
    public String toString() {
        return "Profile{" +
                "profileID=" + profileID +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", birthMonth=" + birthMonth +
                ", birthDay=" + birthDay +
                ", securityQuestion1='" + securityQuestion1 + '\'' +
                ", answer1='" + answer1 + '\'' +
                ", securityQuestion2='" + securityQuestion2 + '\'' +
                ", answer2='" + answer2 + '\'' +
                ", securityQuestion3='" + securityQuestion3 + '\'' +
                ", answer3='" + answer3 + '\'' +
                ", createDate=" + createDate +
                ", modifiedDate=" + modifiedDate +
                '}';
    }
}