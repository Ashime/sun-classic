package com.valiantgaming.databaseserver.database.dao.account;

import com.valiantgaming.databaseserver.database.entity.Profile;
import com.valiantgaming.databaseserver.database.entity.account.Account;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;

import java.util.List;

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

        if(query.execute())
        {
            if(query.getSingleResult().toString().equals("SUCCESS!"))
            {
                return true;
            }
        }

        log.error("SP CreateAccount - " + query.getSingleResult().toString());
        return false;
    }

    public void deactivateAccount(String username, int waitTime)
    {
        StoredProcedureQuery query = session.createNamedStoredProcedureQuery("DeactivateAccount")
                .setParameter("username", username)
                .setParameter("waitTime", waitTime);

        if(query.execute())
        {
            if(!query.getSingleResult().toString().equals("SUCCESS!"))
            {
                log.error("SP DeactivateAccount - " + query.getSingleResult().toString());
            }
        }
    }

    public void deleteAccount(String username)
    {
        StoredProcedureQuery query = session.createNamedStoredProcedureQuery("DeleteAccount")
                .setParameter("username", username);

        if(query.execute())
        {
            if(!query.getSingleResult().toString().equals("SUCCESS!"))
            {
                log.error("SP DeleteAccount - " + query.getSingleResult().toString());
            }
        }
    }

    public List<Account> getDeactivatedAccounts()
    {
        StoredProcedureQuery query = session.createNamedStoredProcedureQuery("GetDeactivatedAccounts");

        if(query.execute())
        {
            return (List<Account>) query.getResultList();
        }

        log.error("SP GetDeactivatedAccounts - Failed to execute!");
        return null;
    }

    public void updateAllPasswords()
    {
        StoredProcedureQuery query = session.createNamedStoredProcedureQuery("UpdateAllPasswords");

        if(query.execute())
        {
            if(query.getSingleResult().toString().contains("FAILED"))
            {
                log.error("SP CreateAccount - " + query.getSingleResult().toString());
            }
        }
    }
}