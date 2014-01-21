package org.sagebionetworks.table.cluster;

public class InstanceUtils {

	public static final String DATABASE_INSTANCE_NAME_TEMPALTE = "%1$s-%2$d-table-%3$d";
	
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
}
