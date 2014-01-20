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
	
	/**
	 * Called by Spring on startup of the factory.
	 */
	public void initialize(){
		// There is nothing to do here if the feature is disabled.
		if(stackConfig.getTableEnabled()){
			// Do we already have a database?
			CreateDBInstanceRequest request = getDefaultCreateDBInstanceRequest();
			// This will be the schema name.
			request.setDBName(stackConfig.getrds.getStackInstanceDatabaseSchema());
			request.setDBInstanceIdentifier(config.getStackInstanceDatabaseIdentifier());
			request.setMasterUsername(stackConfig.getRepositoryDatabaseUsername());
			request.setMasterUserPassword(stackConfig.getRepositoryDatabasePassword());
			// The security group
			request.withDBSecurityGroups(config.getStackDatabaseSecurityGroupName());
			// The parameters.
			request.setDBParameterGroupName(config.getDatabaseParameterGroupName());
		}
	}
	
	/**
	 * Validate the table feature is enabled before making any other call.
	 * 
	 */
	public void validateEnable(){
		if(!stackConfig.getTableEnabled()){
			throw new IllegalStateException("The Table feature is disabled.  To enable this feature set org.sagebionetworks.table.enabled=true.");
		}
	}
	
	/**
	 * If this database instances does not already exist it will be created, otherwise the instance information will be returned.
	 * 
	 * @param request
	 * @return
	 */
	DBInstance createOrGetDatabaseInstance(CreateDBInstanceRequest request){
		// First query for the instance
		try{
			DescribeDBInstancesResult result = client.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(request.getDBInstanceIdentifier()));
			if(result.getDBInstances() == null || result.getDBInstances().size() != 1) throw new IllegalStateException("Did not find exactly one database instances with the identifier: "+request.getDBInstanceIdentifier());
			log.debug("Database: "+request.getDBInstanceIdentifier()+" already exists");
			return result.getDBInstances().get(0);
		}catch(DBInstanceNotFoundException e){
			// This database does not exist to create it
			// Create the database.
			log.debug("Creating database...");
			log.debug(request);
			return client.createDBInstance(request);
		}
	}
	
	/**
	 * Fill out a CreateDBInstanceRequest will all of the default values.
	 * 
	 * @return
	 */
	public static CreateDBInstanceRequest getDefaultCreateDBInstanceRequest(){
		CreateDBInstanceRequest request = new CreateDBInstanceRequest();
		request.setAllocatedStorage(new Integer(5));
		request.setDBInstanceClass(DATABASE_INSTANCE_CLASS_SMALL);
		request.setEngine(DATABASE_ENGINE_MYSQL);
		request.setAvailabilityZone(EC2_AVAILABILITY_ZONE_US_EAST_1D);
		request.setPreferredMaintenanceWindow(PREFERRED_DATABASE_MAINTENANCE_WINDOW_SUNDAY_NIGHT_PDT);
		request.setBackupRetentionPeriod(new Integer(1));
		request.setPreferredBackupWindow(PREFERRED_DATABASE_BACKUP_WINDOW_MIDNIGHT);
		request.setMultiAZ(false);
		request.setEngineVersion(DATABASE_ENGINE_MYSQL_VERSION);
		request.setAutoMinorVersionUpgrade(true);
		request.setLicenseModel(LICENSE_MODEL_GENERAL_PUBLIC);
		// The size of the database should be 10GB
		request.setAllocatedStorage(new Integer(10));
		return request;
	}

}
