package br.ufrn.raszz.persistence;

import org.hibernate.Session;

import java.io.File;

public class SingletonSession 
{
	
	private static Session hibernateSession; 
	
	public static Session getSession(String configName)
	{
		if(hibernateSession != null)
		{
			return hibernateSession;
		}
		else
		{
			hibernateSession = HibernateUtil.getSqlSessionFactory(configName).openSession();
			return hibernateSession;
		}
	}
}
