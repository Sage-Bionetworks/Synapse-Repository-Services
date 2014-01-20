package org.sagebionetworks.table.cluster;

public class Constants {
	
	/**
	 * Small database instance class
	 */
	public static final String DATABASE_INSTANCE_CLASS_SMALL = "db.m1.small";
	
	/**
	 * MySQL database engine.
	 */
	public static final String DATABASE_ENGINE_MYSQL = "MySQL";
	
	/**
	 * MySQL version.
	 */
	public static final String DATABASE_ENGINE_MYSQL_VERSION = "5.5.12";
	/**
	 * us-east-1d
	 */
	public static final String  EC2_AVAILABILITY_ZONE_US_EAST_1D = "us-east-1d";
	
	/**
	 * This window is in UTC.  Monday morning UTC should be Sunday night PDT.
	 */
	public static final String PREFERRED_DATABASE_MAINTENANCE_WINDOW_SUNDAY_NIGHT_PDT = "Mon:07:15-Mon:07:45";
		
	/**
	 * This window is in UTC.  Should be 10 pm - 1 am PDT
	 */
	public static final String PREFERRED_DATABASE_BACKUP_WINDOW_MIDNIGHT = "3:00-6:00";
	
	/**
	 * general-public-license
	 */
	public static final String LICENSE_MODEL_GENERAL_PUBLIC = "general-public-license";

}
