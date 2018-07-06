package org.sagebionetworks.search;

/**
 * Constants used for the search index.
 * 
 * @author jmhill
 *
 */
public class SearchConstants {
	/**
	 * TODO: this needs to be refactored into the awscloudsearch package
	 * The search field names
	 */
	public static final String FIELD_NAME		 	= "name";
	public static final String FIELD_ETAG			= "etag";
	public static final String FIELD_PATH		 	= "path";
	public static final String FIELD_TISSUE			= "tissue";
	public static final String FIELD_REFERENCE 		= "reference";
	public static final String FIELD_PLATFORM 		= "platform";
	public static final String FIELD_NODE_TYPE 		= "node_type";
	public static final String FIELD_DISEASE 		= "disease";
	public static final String FIELD_MODIFIED_BY 	= "modified_by";
	public static final String FIELD_CREATED_BY 	= "created_by";
	public static final String FIELD_UPDATE_ACL 	= "update_acl";
	public static final String FIELD_ACL 			= "acl";
	public static final String FIELD_PARENT_ID 		= "parent_id";
	public static final String FIELD_NUM_SAMPLES 	= "num_samples";
	public static final String FIELD_CREATED_ON 	= "created_on";
	public static final String FIELD_MODIFIED_ON 	= "modified_on";
	public static final String FIELD_BOOST 			= "boost";
	public static final String FIELD_DESCRIPTION 	= "description";
	public static final String FIELD_CONSORTIUM     = "consortium";

	//the id field is not a part of the index but it can be searched like any other.
	public static final String FIELD_ID			 	= "_id";

}
