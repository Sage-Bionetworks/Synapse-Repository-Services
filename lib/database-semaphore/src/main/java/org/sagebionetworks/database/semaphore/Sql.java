package org.sagebionetworks.database.semaphore;

/**
 * SQL constants.
 * 
 */
public class Sql {

	// SEMAPHORE_MASTER
	public static final String TABLE_SEMAPHORE_MASTER = "SEMAPHORE_MASTER";
	public static final String COL_TABLE_SEM_MAST_KEY = "LOCK_KEY";

	// SEMAPHORE_LOCK
	public static final String TABLE_SEMAPHORE_LOCK = "SEMAPHORE_LOCK";
	public static final String COL_TABLE_SEM_LOCK_LOCK_KEY = "LOCK_KEY";
	public static final String COL_TABLE_SEM_LOCK_TOKEN = "TOKEN";
	public static final String COL_TABLE_SEM_LOCK_EXPIRES_ON = "EXPIRES_ON";

}
