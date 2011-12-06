package org.sagebionetworks.web.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sagebionetworks.web.server.servlet.LicenseServiceImpl;
import org.sagebionetworks.web.server.servlet.NcboSearchService;
import org.sagebionetworks.web.server.servlet.NodeServiceImpl;
import org.sagebionetworks.web.server.servlet.ProjectServiceImpl;
import org.sagebionetworks.web.server.servlet.SearchServiceImpl;
import org.sagebionetworks.web.server.servlet.SynapseClientImpl;
import org.sagebionetworks.web.server.servlet.UserAccountServiceImpl;
import org.sagebionetworks.web.server.servlet.filter.RPCValidationFilter;
import org.sagebionetworks.web.server.servlet.filter.TimingFilter;

import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.google.inject.servlet.ServletModule;

/**
 * Binds the service servlets to their paths and any other 
 * Guice binding required on the server side.
 *  
 * @author jmhill
 * 
 */
public class PortalServletModule extends ServletModule {
	
	private static Logger logger = Logger.getLogger(PortalServletModule.class.getName());

	@Override
	protected void configureServlets() {
//		// This is not working yet
//		filter("/Portal/*").through(SimpleAuthFilter.class);
//		bind(SimpleAuthFilter.class).in(Singleton.class);
		
		// filter all call through this filter
		filter("/Portal/*").through(TimingFilter.class);
		bind(TimingFilter.class).in(Singleton.class);
		// This supports RPC
		filter("/Portal/*").through(RPCValidationFilter.class);
		bind(RPCValidationFilter.class).in(Singleton.class);

		// Setup the Search service
		bind(SynapseClientImpl.class).in(Singleton.class);
		serve("/Portal/synapse").with(SynapseClientImpl.class);
		
		// Setup the Search service
		bind(SearchServiceImpl.class).in(Singleton.class);
		serve("/Portal/search").with(SearchServiceImpl.class);
		// setup the project service
		bind(ProjectServiceImpl.class).in(Singleton.class);
		serve("/Portal/project").with(ProjectServiceImpl.class);
	
		// setup the node service
		bind(NodeServiceImpl.class).in(Singleton.class);
		serve("/Portal/node").with(NodeServiceImpl.class);
			
		// Setup the License service mapping
		bind(LicenseServiceImpl.class).in(Singleton.class);
		serve("/Portal/license").with(LicenseServiceImpl.class);
		
		// Setup the User Account service mapping
		bind(UserAccountServiceImpl.class).in(Singleton.class);
		serve("/Portal/users").with(UserAccountServiceImpl.class);
	
		// setup the NCBO servlet
		bind(NcboSearchService.class).in(Singleton.class);
		serve("/Portal/ncbo/search").with(NcboSearchService.class);
		
		// The Rest template provider should be a singleton.
		bind(RestTemplateProviderImpl.class).in(Singleton.class);
		bind(RestTemplateProvider.class).to(RestTemplateProviderImpl.class);
		// Bind the properties from the config file
		bindPropertiesFromFile("ServerConstants.properties");
		
		// Bind the ConlumnConfig to singleton
		bind(ColumnConfigProvider.class).in(Singleton.class);
	}
	
	
	
	/**
	 * Attempt to bind all properties found in the given property file.
	 * The property file should be on the classpath.
	 * @param resourceName
	 */
	private void bindPropertiesFromFile(String resourceName){
		InputStream in = PortalServletModule.class.getClassLoader().getResourceAsStream(resourceName);
		if(in != null){
			try{
				Properties props = new Properties();
				// First load the properties from the server config file.
				props.load(in);
				// Override any property that is in the System properties.
				Properties systemProps = System.getProperties();
				Iterator<Object> it = systemProps.keySet().iterator();
				while(it.hasNext()){
					Object obKey = it.next();
					if(obKey instanceof String){
						String key = (String) obKey;
						// Add all system properites
						String newValue = systemProps.getProperty(key);
						String previous = (String) props.setProperty(key, newValue);
						if(previous != null){
							logger.info("Overriding a ServerConstants.properties key: "+key+" with a value from System.properties(). New value: "+newValue);
						}
					}
				}
				// Bind the properties
				Names.bindProperties(binder(),props);
			} catch (IOException e) {
				logger.log(Level.SEVERE, e.getMessage(), e);
			}finally{
				try {
					in.close();
				} catch (IOException e) {}
			}
		}else{
			logger.severe("Cannot find property file on classpath: "+resourceName); 
		}
	}

}
