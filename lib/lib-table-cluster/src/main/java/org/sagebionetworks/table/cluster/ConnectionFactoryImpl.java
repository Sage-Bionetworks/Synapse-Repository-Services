package org.sagebionetworks.table.cluster;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBInstance;

/**
 * Note: For the first pass at this feature we are only using one database.
 * This will be extended in the future.
 * 
 * @author jmhill
 *
 */
public class ConnectionFactoryImpl implements ConnectionFactory {
	
	Logger log = LogManager.getLogger(ConnectionFactoryImpl.class);
	
	@Autowired
	AmazonRDSClient awsRDSClient;
	@Autowired
	InstanceDiscovery instanceDiscovery;
	
	@Autowired
	private StackConfiguration stackConfig;
	
	@Override
	public SimpleJdbcTemplate getConnection(String tableId) {
		// This method can only be called in 
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * This is called when the Spring bean is initialized.
	 */
	public void initialize(){
		// There is nothing to do if the table feature is not enabled.
		if(stackConfig.getTableEnabled()){
			// The features is enabled so we must find all database instances that we can use
			List<DBInstance> instances = instanceDiscovery.discoverAllInstances();
			if(instances == null || instances.isEmpty()) throw new IllegalArgumentException("Did not find at least one database instances.  Expected at least one instances: "+InstanceUtils.createDatabaseInstanceIdentifier(0));
			
		}else{
			log.debug("The table feature is disabled and cannot be used");
		}
	}

}
