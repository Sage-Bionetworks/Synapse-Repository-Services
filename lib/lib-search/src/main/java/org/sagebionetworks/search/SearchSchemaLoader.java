package org.sagebionetworks.search;

import static org.sagebionetworks.search.SearchConstants.FIELD_ACL;
import static org.sagebionetworks.search.SearchConstants.FIELD_BOOST;
import static org.sagebionetworks.search.SearchConstants.FIELD_CONSORTIUM;
import static org.sagebionetworks.search.SearchConstants.FIELD_CREATED_BY;
import static org.sagebionetworks.search.SearchConstants.FIELD_CREATED_ON;
import static org.sagebionetworks.search.SearchConstants.FIELD_DESCRIPTION;
import static org.sagebionetworks.search.SearchConstants.FIELD_DISEASE;
import static org.sagebionetworks.search.SearchConstants.FIELD_ETAG;
import static org.sagebionetworks.search.SearchConstants.FIELD_MODIFIED_BY;
import static org.sagebionetworks.search.SearchConstants.FIELD_MODIFIED_ON;
import static org.sagebionetworks.search.SearchConstants.FIELD_NAME;
import static org.sagebionetworks.search.SearchConstants.FIELD_NODE_TYPE;
import static org.sagebionetworks.search.SearchConstants.FIELD_NUM_SAMPLES;
import static org.sagebionetworks.search.SearchConstants.FIELD_PARENT_ID;
import static org.sagebionetworks.search.SearchConstants.FIELD_PLATFORM;
import static org.sagebionetworks.search.SearchConstants.FIELD_REFERENCE;
import static org.sagebionetworks.search.SearchConstants.FIELD_TISSUE;
import static org.sagebionetworks.search.SearchConstants.FIELD_UPDATE_ACL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.amazonaws.services.cloudsearchv2.model.IndexField;
import com.amazonaws.services.cloudsearchv2.model.IndexFieldType;
import com.amazonaws.services.cloudsearchv2.model.LiteralArrayOptions;
import com.amazonaws.services.cloudsearchv2.model.LiteralOptions;
import com.amazonaws.services.cloudsearchv2.model.TextArrayOptions;
import com.amazonaws.services.cloudsearchv2.model.TextOptions;

/**
 * Load the search schema.
 * 
 * 
 * @author jmhill
 *
 */
public class SearchSchemaLoader {
	/*
	 * CloudSearchV2 defaults to this analysis scheme if not specified,
	 * but specifying it will make equals() function for comparing
	 * the created IndexFields to ones retrieved from AWS work.
	 */
	private static String DEFAULT_TEXT_ANALYSIS_SCHEME = "_en_default_";
	/**
	 * For now we are defining the schema in code.
	 * We can serialize this to file in the future if we decide to do so.
	 * 
	 * @return
	 */



	public static List<IndexField> loadSearchDomainSchema() {
		return SEARCH_SCHEMA_FIELDS;
	}
}
