package br.ufrn.raszz.persistence;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

public class HibernateUtil {
	private static SessionFactory sessionFactory = null;

	private static SessionFactory buildSessionFactory(String config){
		try{
			// Create ServiceRegistry from hibernate_swt.cfg.xml
			ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
					.configure("hibernate.cfg.xml").build();

			// Create a metadata sources using the service registry
			Metadata metadata = new MetadataSources(serviceRegistry)
					.getMetadataBuilder().build();

			return metadata.getSessionFactoryBuilder().build();
		} catch (Exception e){
			e.printStackTrace();
			throw new ExceptionInInitializerError(e);
		}
	}

	public static SessionFactory getSqlSessionFactory(String config){
		if (sessionFactory == null)
			sessionFactory = buildSessionFactory(config);
		return sessionFactory;
	}

	public static void shutdown(){
		if (sessionFactory != null) {
			sessionFactory.close();
			sessionFactory = null;
		}
	}
}