package com.valiantgaming.databaseserver.database;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

@Log4j2
public class HibernateSession
{
    @Getter
    private static Session session = null;
    private static SessionFactory sessionFactory = null;
    private static StandardServiceRegistry registry = null;

    /**
     * Create Hibernate's SessionFactory to setup multiple Sessions.
     */
    @SneakyThrows
    public static void createSessionFactory()
    {
        if (sessionFactory == null)
        {
                // Create registry
                registry = new StandardServiceRegistryBuilder().configure().build();
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
        session = sessionFactory.openSession();
        return session;
    }
}
