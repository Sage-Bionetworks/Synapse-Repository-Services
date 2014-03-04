package org.sagebionetworks.table.cluster;

import java.util.List;

import com.amazonaws.services.rds.model.DBInstance;

/**
 * Discovers database instances that are available.
 * 
 * @author jmhill
 *
 */
public interface InstanceDiscovery {
	
	public static String RDS_STATUS_AVAILABLE = "available";

	/**
	 * Discover all of the instances that are currently available.
	 * 
	 * This method will always return at least one instances (even if it must be created to do so).
	 * 
	 * @return
	 */
	List<DBInstance> discoverAllInstances();
}
