package org.sagebionetworks.web.server;

/**
 * Constant property keys
 * 
 * @author jmhill
 *
 */
public class ServerConstants {

	/**
	 * The property key used for the list of default dataset columns
	 */
	public static final String KEY_DEFAULT_DATASET_COLS =  "org.sagebionetworks.all.datasets.default.columns";
	
	/**
	 * The property key used for the list of default dataset layers
	 */
	public static final String KEY_DEFAULT_LAYER_COLS =  "org.sagebionetworks.all.datasets.default.layers";
	
	/**
	 * The property key that tell what column configuration xml file to be used.
	 */
	public static final String KEY_COLUMN_CONFIG_XML_FILE = "org.sagebionetworks.column.config.xml.resource";
	
	
	/**
	 * The property key that tells where the root of the rest API can be found;
	 */
	public static final String KEY_REST_API_ROOT_URL = "org.sagebionetworks.rest.api.root.url";
}
