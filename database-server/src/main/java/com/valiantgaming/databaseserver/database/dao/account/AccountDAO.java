package com.valiantgaming.databaseserver.database.dao.account;

import com.valiantgaming.databaseserver.database.entity.account.Account;
import com.valiantgaming.databaseserver.database.entity.profile.Profile;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;

@Log4j2
public class AccountDAO
{
    private Session session;

    public AccountDAO(Session session)
    {
        this.session = session;
    }

    public boolean createAccount(Account account, Profile profile)
    {
        StoredProcedureQuery query = session.createNamedStoredProcedureQuery("CreateAccount")
                .setParameter("username", account.getUsername())
                .setParameter("password", account.getPassword())
                .setParameter("salt", account.getSalt())
                .setParameter("email", account.getEmail())
                .setParameter("firstName", profile.getFirstName())
                .setParameter("lastName", profile.getLastName())
                .setParameter("birthMonth", profile.getBirthMonth())
                .setParameter("birthDay", profile.getBirthDay())
                .setParameter("securityQ1", profile.getSecurityQuestion1())
                .setParameter("answer1", profile.getAnswer1())
                .setParameter("securityQ2", profile.getSecurityQuestion2())
                .setParameter("answer2", profile.getAnswer2())
                .setParameter("securityQ3", profile.getSecurityQuestion3())
                .setParameter("answer3", profile.getAnswer3());

        boolean status = query.execute();

        if(status)
            if(query.getSingleResult().toString().equals("SUCCESS!"))
                return true;

        log.error("SP CreateAccount - " + query.getSingleResult().toString());
        return false;
    }

    public void updateAllPasswords()
    {
        StoredProcedureQuery query = session.createNamedStoredProcedureQuery("UpdateAllPasswords");

        boolean status = query.execute();

        if(status)
            if(query.getSingleResult().toString().contains("UNSUCCESSFUL"))
                log.error("SP CreateAccount - " + query.getSingleResult().toString());
    }
}