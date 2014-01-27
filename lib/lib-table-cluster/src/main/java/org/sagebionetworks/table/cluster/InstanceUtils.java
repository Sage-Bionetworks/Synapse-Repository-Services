package org.sagebionetworks.table.cluster;

import org.sagebionetworks.StackConfiguration;

public class InstanceUtils {

	public static final String DATABASE_INSTANCE_NAME_TEMPALTE = "%1$s-%2$d-table-%3$d";
	
	private static StackConfiguration config = new StackConfiguration();
	
	/**
	 * Create the database instances stack identifier.
	 * @param stack
	 * @param instanceNumber
	 * @param index
	 * @return
	 */
	public static String createDatabaseInstanceIdentifier(String stack, int instanceNumber, int index){
		return String.format(DATABASE_INSTANCE_NAME_TEMPALTE, stack, instanceNumber, index);
	}
	
	/**
	 * Create the database instances stack identifier from the stack configuration.
	 * 
	 * @param index
	 * @return
	 */
	public static String createDatabaseInstanceIdentifier(int index){
		return createDatabaseInstanceIdentifier(StackConfiguration.getStackInstance(), config.getStackInstanceNumber(), index);
	}
}
