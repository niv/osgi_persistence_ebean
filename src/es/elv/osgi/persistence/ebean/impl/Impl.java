package es.elv.osgi.persistence.ebean.impl;

import java.util.Collections;
import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.NamingConvention;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebeaninternal.server.lib.ShutdownManager;

import es.elv.osgi.persistence.ebean.PersistenceService;
import es.elv.osgi.persistence.ebean.PersistentModelClassService;

public class Impl implements ManagedService, PersistenceService {
	private Object syncObject = new Object();

	private List<Class<?>> annotatedClassList =
		Collections.synchronizedList(new LinkedList<Class<?>>());

	private EbeanServer server;

	private String driver;
	private String username;
	private String password;
	private String url;
	private String heartbeatSql;
	private NamingConvention namingConvention;
	private boolean ddlgenerate;
	private boolean ddlrun;
	private boolean showSql;

	private boolean autofetchProfiling, autofetchQueryTuning;
	private double profilingRate;

	private boolean scheduleUpdate = false;

	public void addMCS(PersistentModelClassService pmc) {
		synchronized (syncObject) {
			for (Class<?> clazz : pmc.getAnnotatedModels())
				if (!annotatedClassList.contains(clazz)) {
					annotatedClassList.add(clazz);
					// System.err.println("MCS: Added class " + clazz.getName());
					scheduleUpdate = true;
				}
		}
	}

	public void removeMCS(PersistentModelClassService pmc) {
		synchronized (syncObject) {
			for (Class<?> clazz : pmc.getAnnotatedModels()) {
				annotatedClassList.remove(clazz);
				scheduleUpdate = true;
				// System.err.println("MCS: Removed class " + clazz.getName());
			}

		}
	}

	public void deactivate(ComponentContext ctx) {
		annotatedClassList.clear();
		ShutdownManager.shutdown();
		server = null;
	}

	@Override
	public void updated(@SuppressWarnings("rawtypes") Dictionary v) throws ConfigurationException {
		if (v == null)
			return;

		driver = (String) v.get("connection.driver_class");
		if (driver == null)
			throw new ConfigurationException("driver", "cannot be null");

		username = (String) v.get("connection.username");
		if (username == null)
			throw new ConfigurationException("username", "cannot be null");

		password = (String) v.get("connection.password");
		if (password == null)
			throw new ConfigurationException("password", "cannot be null");

		url = (String) v.get("connection.url");
		if (url == null)
			throw new ConfigurationException("url", "cannot be null");

		heartbeatSql = (String) v.get("heartbeatSql");
		if (heartbeatSql == null)
			throw new ConfigurationException("heartbeatSql", "cannot be null");


		String nc = (String) v.get("namingConvention");
		if (nc == null)
			throw new ConfigurationException("nc", "cannot be null");

		try {
			namingConvention = (NamingConvention) Class.forName(nc).newInstance();
		} catch (InstantiationException e) {
			throw new ConfigurationException("namingConvention", "cannot create", e);
		} catch (IllegalAccessException e) {
			throw new ConfigurationException("namingConvention", "illegal access", e);
		} catch (ClassNotFoundException e) {
			throw new ConfigurationException("namingConvention", "class not found", e);
		}

		ddlgenerate = (Boolean) v.get("ddlgenerate");
		ddlrun = (Boolean) v.get("ddlrun");
		showSql = (Boolean) v.get("show_sql");
		autofetchProfiling = (Boolean) v.get("autofetch.profile");
		autofetchQueryTuning = (Boolean) v.get("autofetch.querytuning");
		profilingRate = (Double) v.get("autofetch.profiling_rate");
	}

	private void reconfigure() {
		if (driver == null || username == null || password == null || url == null)
			return;

		if (!scheduleUpdate)
			return;

		synchronized (syncObject) {
			// System.err.println("Reconfiguring with the following " + annotatedClassList.size() + " classes: ");
			// for (Class<?> clazz : annotatedClassList)
			//	System.err.println("  "  + clazz.getName());

			ServerConfig config = new ServerConfig();
			config.setName("es.elv.nwnx2.jvm.persistence.ebean.persistence");

			config.setClasses(annotatedClassList);

			DataSourceConfig dsConfig = new DataSourceConfig();
			dsConfig.setDriver(driver);
			dsConfig.setUsername(username);
			dsConfig.setPassword(password);
			dsConfig.setUrl(url);
			if (!heartbeatSql.equals(""))
				dsConfig.setHeartbeatSql(heartbeatSql);
			config.setDataSourceConfig(dsConfig);

			config.setDebugSql(showSql);

			config.getAutofetchConfig().setProfiling(autofetchProfiling);
			config.getAutofetchConfig().setQueryTuning(autofetchQueryTuning);
			config.getAutofetchConfig().setProfilingRate(profilingRate);

			config.setRegister(false);
			config.setDefaultServer(false);

			config.setVanillaMode(true);
			config.setVanillaRefMode(true);

			config.setDdlGenerate(ddlgenerate);
			config.setDdlRun(ddlrun);

			config.setNamingConvention(namingConvention);

			ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
			server = EbeanServerFactory.create(config);
			Thread.currentThread().setContextClassLoader(oldClassLoader);

			scheduleUpdate = false;
		}
	}

	@Override
	public EbeanServer get() {
		if (scheduleUpdate)
			reconfigure();

		assert (server != null);

		synchronized (syncObject) {
			return server;
		}
	}

	@Override
	public Class<?>[] getAnnotatedClassList() {
		return annotatedClassList.toArray(new Class<?>[] {});
	}
}
