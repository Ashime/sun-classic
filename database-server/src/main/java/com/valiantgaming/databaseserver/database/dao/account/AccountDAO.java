package com.valiantgaming.databaseserver.database.dao.account;

import com.valiantgaming.commons.security.crypt.ARGON2;
import com.valiantgaming.databaseserver.database.entity.Profile;
import com.valiantgaming.databaseserver.database.entity.account.Account;
import jakarta.persistence.StoredProcedureQuery;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;

import java.util.List;

@Log4j2
public class AccountDAO implements AutoCloseable
{
    private Session session;

    @Getter
    private String resultMessage;

    public AccountDAO(Session session)
    {
        this.session = session;
    }

    @Override
    public void close()
    {
        session.close();
    }

    public boolean authenticateAccount(String username, String password)
    {
        Account account = getAccountCredentials(username);

        // Still asked of ARGON2 even when the account doesn't exist (against a null-safe "false"), and the SP
        // is still called regardless, so a nonexistent username doesn't resolve any faster than a wrong password -
        // AuthenticateAccount independently re-checks whether the account exists.
        boolean passwordMatched = account != null && new ARGON2().validatePassword(account.getPassword(), password);

        StoredProcedureQuery query = session.createNamedStoredProcedureQuery("AuthenticateAccount")
                .setParameter("username", username)
                .setParameter("passwordMatched", passwordMatched);

        if(query.execute())
        {
            String result = query.getSingleResult().toString();

            if(result.equals("SUCCESS!"))
            {
                return true;
            }

            resultMessage = result;
        }

        log.error("SP AuthenticateAccount - {}", resultMessage);
        return false;
    }

    public boolean createAccount(Account account, Profile profile)
    {
        StoredProcedureQuery query = session.createNamedStoredProcedureQuery("CreateAccount")
                .setParameter("username", account.getUsername())
                .setParameter("password", account.getPassword())
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
            String result = query.getSingleResult().toString();

            if(result.equals("SUCCESS!"))
            {
                return true;
            }

            resultMessage = result;
        }

        log.error("SP CreateAccount - {}", resultMessage);
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

    public Account getAccountCredentials(String username)
    {
        StoredProcedureQuery query = session.createNamedStoredProcedureQuery("GetAccountCredentials");
        query.setParameter("username", username);

        if(query.execute())
        {
            List<Account> results = query.getResultList();

            if(!results.isEmpty())
            {
                return results.get(0);
            }
        }

        return null;
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