package com.valiantgaming.databaseserver.database;

import jakarta.validation.constraints.NotNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import static com.valiantgaming.commons.utility.Utility.requireEnv;


@Log4j2
public class HibernateSession
{
    private static SessionFactory sessionFactory = null;
    private static StandardServiceRegistry registry = null;

    /**
     * Create Hibernate's SessionFactory to setup multiple Sessions.
     */
    @SneakyThrows
    public static void createSessionFactory()
    {
        @NotNull
        String dbUsername = requireEnv("DB_USERNAME");
        @NotNull
        String dbPassword = requireEnv("DB_PASSWORD");

        if (sessionFactory == null)
        {
            StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder().configure();

            builder.applySetting("hibernate.connection.username", dbUsername);
            builder.applySetting("hibernate.connection.password", dbPassword);

            // Build registry
            registry = builder.build();
            // Create MetadataSources
            MetadataSources sources = new MetadataSources(registry);
            // Create Metadata
            Metadata metadata = sources.getMetadataBuilder().build();
            // Create SessionFactory
            sessionFactory = metadata.getSessionFactoryBuilder().build();
        }
    }

    /**
     * Shuts down the SessionFactory by destroying the registry.
     */
    public static void shutdown()
    {
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    /**
     * Create a unique session by opening the SessionFactory.
     *
     * @return Returns a Session.
     */
    public static Session createSession()
    {
        return sessionFactory.openSession();
    }
}