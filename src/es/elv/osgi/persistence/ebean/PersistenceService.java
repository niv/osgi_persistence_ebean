package es.elv.osgi.persistence.ebean;

import com.avaje.ebean.EbeanServer;

public interface PersistenceService {
	public EbeanServer get();
	
	public Class<?>[] getAnnotatedClassList();
}
