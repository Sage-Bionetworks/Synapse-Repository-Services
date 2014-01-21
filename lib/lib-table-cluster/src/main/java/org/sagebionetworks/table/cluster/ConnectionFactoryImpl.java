package org.sagebionetworks.table.cluster;

import static  org.sagebionetworks.table.cluster.Constants.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.CreateDBInstanceRequest;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBInstanceNotFoundException;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;

/**
 * Note: For the first pass at this feature we are only using one database.
 * This will be extended in the future.
 * 
 * @author jmhill
 *
 */
public class ConnectionFactoryImpl implements ConnectionFactory{
	
	Logger log = LogManager.getLogger(ConnectionFactoryImpl.class);
	
	@Autowired
	AmazonRDSClient client;
	
	@Autowired
	private StackConfiguration stackConfig;
	
	@Override
	public SimpleJdbcTemplate getConnection(String tableId) {
		// This method can only be called in 
		// TODO Auto-generated method stub
		return null;
	}
	

}
