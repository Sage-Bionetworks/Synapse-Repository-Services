package org.sagebionetworks.table.cluster;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBInstance;
import com.amazonaws.services.rds.model.DBInstanceNotFoundException;
import com.amazonaws.services.rds.model.DescribeDBInstancesRequest;
import com.amazonaws.services.rds.model.DescribeDBInstancesResult;

/**
 * A simple naming convention is used to name each instances:
 * '<stack>-<instance_number>-table-n'
 * 
 * Where 'n' is the instances number of the database starting at zero.  For example,
 * the table database instances for prod-28 will have the following names:
 * 'prod-28-table-0'
 * 'prod-28-table-1'
 * 'prod-28-table-...'
 * 
 * This discovery implementation will query AWS looking for all instances sequentially starting at zero.
 * 
 * @author jmhill
 *
 */
public class InstanceDiscoveryImpl implements  InstanceDiscovery {
	
	Logger log = LogManager.getLogger(InstanceDiscoveryImpl.class);

	
	@Autowired
	AmazonRDSClient awsRDSClient;
	
	private volatile int maxNumberFound = 0;
	
	
	/**
	 * This method is designed to be called repeatedly over time to discover new instances and lost instances.
	 */
	@Override
	public List<DBInstance> discoverAllInstances() {
		List<DBInstance> list = new LinkedList<DBInstance>();
		int index = 0;
		DBInstance instance = null;
		do{
			// Does this instance exist?
			String databaseIdentifier = InstanceUtils.createDatabaseInstanceIdentifier(index);
			instance = getInstanceIfExists(databaseIdentifier);
			if(instance != null){
				// We have an instance but is it ready?
				if("available".equals(instance.getDBInstanceStatus())){
					list.add(instance);
				}else{
					log.debug("Found a database: "+instance.getDBInstanceIdentifier()+" but could not use it because its status was: "+instance.getDBInstanceStatus());
				}
			}
			index++;
		}while(instance != null || index < maxNumberFound);
		maxNumberFound = Math.max(index, maxNumberFound);
		return list;
	}
	
	
	
	/**
	 * If this database instances does not already exist it will be created, otherwise the instance information will be returned.
	 * 
	 * @param request
	 * @return
	 */
	DBInstance getInstanceIfExists(String databaseIdentifer){
		// First query for the instance
		try{
			DescribeDBInstancesResult result = awsRDSClient.describeDBInstances(new DescribeDBInstancesRequest().withDBInstanceIdentifier(databaseIdentifer));
			if(result.getDBInstances() == null || result.getDBInstances().size() != 1) throw new IllegalStateException("Did not find exactly one database instances with the identifier: "+databaseIdentifer);
			DBInstance instance = result.getDBInstances().get(0);
			log.debug("Found database instances: "+instance.getDBInstanceIdentifier());
			return instance; 
		}catch(DBInstanceNotFoundException e){
			// This database does not exist to create it
			// Create the database.
			log.debug("Database instances cannot be found: "+databaseIdentifer);
			return null;
		}
	}

}
