package org.sagebionetworks.tool.migration;

/**
 * Constants used by the tool.
 * @author jmhill
 *
 */
public class Constants {
	
	public static final String ENTITY			= "entity";
	public static final String ENTITY_DOT		= ENTITY+".";
	public static final String ENTITY_ID		= "id";
	public static final String ENTITY_E_TAG		= "eTag";
	public static final String ENTITY_PARENT_ID	= "parentId";
	
	public static final String ENTITY_DOT_ID		= ENTITY_DOT+ENTITY_ID;
	public static final String ENTITY_DOT_E_TAG		= ENTITY_DOT+ENTITY_E_TAG;
	public static final String ENTITY_DOT_PARENT_ID	= ENTITY_DOT+ENTITY_PARENT_ID;
	
	public static final String LIMIT		= "limit";
	public static final String OFFSET		= "offset";
	
	public static final String ENTITY_NAME		= "name";
	
	public static final String ROOT_ENTITY_NAME = "root";
	
	public static final String JSON_KEY_RESULTS					= "results";
	public static final String JSON_KEY_TOTAL_NUMBER_OF_RESULTS	= "totalNumberOfResults";
	
	public static final long MAX_PAGE_SIZE = 2000;
	
	/**
	 * How much time (MS) should we sleep between web service calls.
	 */
	public static final long MS_BETWEEN_SYNPASE_CALLS = 100;
	
	public static long NANO_SECS_PER_MIL_SEC = 1000000;

}
