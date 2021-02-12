package org.sagebionetworks.repo.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Constants for nodes.
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
	 * The name of the column for benefactor Id.
	 */
	public static final String COL_BENEFACTOR_ID = "benefactorId";
	/**
	 * The name of the name column.
	 */
	public static final String COL_NAME = "name";
	
	/**
	 * The name of the type column.
	 */
	public static final String COLUMN_LAYER_TYPE = "type";

	
	/**
	 * Forward slash should be the prefix of a node's path.
	 */
	public static final String PATH_PREFIX = "/";
	
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
	
	/**
	 * The default versionLabel for nodes
	 */
	public static final String DEFAULT_VERSION_LABEL = DEFAULT_VERSION_NUMBER.toString();
	
	/**
	 * A zero-ed etag
	 */
	public static final String ZERO_E_TAG = new UUID(0L, 0L).toString();
	
	/**
	 * All bootstrap nodes.
	 *
	 */
	public enum BOOTSTRAP_NODES {
		
		ROOT(4489L),
		TRASH(1681355L);
		
		BOOTSTRAP_NODES(long id) {
			this.id = id;
		}
		private Long id;
		
		public Long getId() {
			return id;
		}
		
		/**
		 * Get all of the bootstrap node IDs.
		 * @return
		 */
		public static List<Long> getAllBootstrapIds(){
			List<Long> ids = new ArrayList<Long>(BOOTSTRAP_NODES.values().length);
			for(BOOTSTRAP_NODES node: BOOTSTRAP_NODES.values()) {
				ids.add(node.getId());
			}
			return ids;
		}
	}
	
	/**
	 * Max path depth for a node hierarchy. Note: The root node counts as one.
	 */
	public static final int MAX_PATH_DEPTH = 50;
	public static final int MAX_PATH_DEPTH_PLUS_ONE = MAX_PATH_DEPTH+1;
	
}
