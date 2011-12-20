package org.sagebionetworks.repo.model;

/**
 * Constants for nodes.
 * @author jmhill
 *
 */
public class NodeConstants {
	
	/**
	 * The name of the id column
	 */
	public static final String COL_ID = "id";
	/**
	 * The name of the column for parentId.
	 */
	public static final String COL_PARENT_ID = "parentId";
	/**
	 * The name of the name column.
	 */
	public static final String COL_NAME = "name";
	
	/**
	 * The name of the type column.
	 */
	public static final String COLUMN_LAYER_TYPE = "type";
	
	/**
	 * The default versionLabel for nodes
	 */
	public static final String DEFAULT_VERSION_LABEL = "0.0.0";
	
	/**
	 * The versionLabel prefix for auto-created versions
	 */
	public static final String AUTOCREATED_VERSION_LABEL_PREFIX = "autoVersionBumpFrom_";
	
	/**
	 * Forward slash should be the prefix of a node's path.
	 */
	public static final String PATH_PREFIX = "/";
	
	public static final String BOOTSTRAP_USERNAME = "bootstrap";
	
	/**
	 * The path of the root folder
	 */
	public static final String ROOT_FOLDER_PATH = "/root";
	/**
	 * The path of end user license agreements (eula)
	 */
	public static final String EULA_FOLDER_PATH = ROOT_FOLDER_PATH+"/eulas";
	
	/**
	 * The path of end user license agreements
	 */
	public static final String AGREEMENT_FOLDER_PATH = ROOT_FOLDER_PATH+"/agreements";
	
	public static final Long DEFAULT_VERSION_NUMBER = new Long(1);
	
}
