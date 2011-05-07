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
	 * The property key used to list additional datsets columns
	 */
	public static final String KEY_ADDITIONAL_DATASET_COLS = "org.sagebionetworks.all.datasets.additional.columns";
	
	/**
	 * The property key used for the list of default dataset layers
	 */
	public static final String KEY_DEFAULT_LAYER_COLS =  "org.sagebionetworks.all.datasets.default.layers";
	
	/**
	 * The property key that tell what column configuration xml file to be used.
	 */
	public static final String KEY_COLUMN_CONFIG_XML_FILE = "org.sagebionetworks.column.config.xml.resource";
	
	/**
	 * The property key that tell what FilterEnumeration.xml file to load.
	 */
	public static final String KEY_FILTER_ENUMERATION_CONFIG_XML_FILE = "org.sagebionetworks.fileter.enumeration.xml.resource";
	
	
	/**
	 * The property key that tells where the rest API service endpoint is.
	 */
	public static final String KEY_REST_API_ENDPOINT = "org.sagebionetworks.rest.api.endpoint";
	
	/**
	 * The property key for the rest api servlet prefix.
	 */
	public static final String KEY_REST_API_SERVLET_PREFIX = "org.sagebionetworks.rest.api.servlet.prefix";
	
	/**
	 * The property key that tells where the auth API service endpoint is.
	 */
	public static final String KEY_AUTH_API_ENDPOINT = "org.sagebionetworks.auth.api.endpoint";
	
	/**
	 * The property key for the auth api servlet prefix.
	 */
	public static final String KEY_AUTH_API_SERVLET_PREFIX = "org.sagebionetworks.auth.api.servlet.prefix";
	

}
