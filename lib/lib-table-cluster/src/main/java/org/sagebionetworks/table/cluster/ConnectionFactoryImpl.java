package org.sagebionetworks.table.cluster;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

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
	/**
	 * This is the repository datasource.
	 */
	@Autowired
	BasicDataSource dataSourcePool;
	/**
	 * Note: This field will be remove when we actually have more than one connection.
	 * It is a simple way to get the functionality up and running without adding full
	 * support for a load balancing.
	 */
	private BasicDataSource singleConnectionPool;

	@Autowired
	private StackConfiguration stackConfig;
	
	@Override
	public TableIndexDAO getConnection(String tableId) {
		validateEnable();
		// Setup a dynamic dao that can still use transactions
		// This proxy is the 'glue' that  allows the SimpleJDBCTempate to participate in transactions
		TransactionAwareDataSourceProxy dataSourceProxy = new TransactionAwareDataSourceProxy(singleConnectionPool);
		// Give the proxy to both the template and transaction manager.
		SimpleJdbcTemplate template = new SimpleJdbcTemplate(singleConnectionPool);
		DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSourceProxy);
		// Create a new DAO for this call.
		return new TableIndexDAOImpl(template, transactionManager);	
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
			
			// This will be improved in the future.  For now we just use the first database we find
			DBInstance instance = instances.get(0);
			// Use the one instance to create a single connection pool
			singleConnectionPool = InstanceUtils.createNewDatabaseConnectionPool(stackConfig, instance);			
		}else{
			log.debug("The table feature is disabled and cannot be used");
		}
	}
	
	/**
	 * Validate that the table feature is enabled.
	 */
	private void validateEnable(){
		if(!stackConfig.getTableEnabled()){
			throw new IllegalArgumentException("The table feature is disabled (org.sagebionetworks.table.enabled=false) so this method is not available.");
		}
	}
	
	/**
	 * Spring will calls this method when this bean is destroyed.
	 * This is our chance to shutdown the database connection pools.
	 * @throws SQLException 
	 */
	public void close() throws SQLException{
		if(singleConnectionPool != null){
			log.debug("Closing connection pool to: "+singleConnectionPool.getUrl());
			singleConnectionPool.close();
		}
	}

}
