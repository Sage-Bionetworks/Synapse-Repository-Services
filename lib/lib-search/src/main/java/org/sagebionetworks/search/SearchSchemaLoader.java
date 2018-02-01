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
	public static final List<IndexField> SEARCH_SCHEMA_FIELDS;
	static {

		//can search values, faceting enabled for this index. index values not be returned in search results.
		LiteralOptions literalOptionsReturnDisabled = new LiteralOptions().withSearchEnabled(true).withFacetEnabled(true).withReturnEnabled(false);
		LiteralArrayOptions literalArrayOptionsReturnDisabled = new LiteralArrayOptions().withSearchEnabled(true).withFacetEnabled(true).withReturnEnabled(false);

		//search, facet, and return all enabled
		LiteralOptions literalOptionsReturnEnabled = new LiteralOptions().withReturnEnabled(true).withSearchEnabled(true).withFacetEnabled(true);
		LiteralArrayOptions literalArrayOptionsReturnEnabled = new LiteralArrayOptions().withReturnEnabled(true).withSearchEnabled(true).withFacetEnabled(true);

		// faceting is disabled because all values are unique
		LiteralOptions literalOptionsFacetDisabled = new LiteralOptions().withReturnEnabled(true).withSearchEnabled(true).withFacetEnabled(false);


		List<IndexField> list = new ArrayList<>();
		// Literal fields to be returned in Search Results
		list.add(new IndexField().withIndexFieldName(FIELD_ETAG).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(literalOptionsFacetDisabled));
		// Free text fields to be returned in Search Results
		list.add(new IndexField().withIndexFieldName(FIELD_NAME).withIndexFieldType(IndexFieldType.Text).withTextOptions(new TextOptions().withReturnEnabled(true).withAnalysisScheme(DEFAULT_TEXT_ANALYSIS_SCHEME)));
		list.add(new IndexField().withIndexFieldName(FIELD_DESCRIPTION).withIndexFieldType(IndexFieldType.Text).withTextOptions(new TextOptions().withReturnEnabled(true).withAnalysisScheme(DEFAULT_TEXT_ANALYSIS_SCHEME)));
		list.add(new IndexField().withIndexFieldName(FIELD_BOOST).withIndexFieldType(IndexFieldType.TextArray).withTextArrayOptions(new TextArrayOptions().withReturnEnabled(false).withAnalysisScheme(DEFAULT_TEXT_ANALYSIS_SCHEME)));
		// Numeric fields (by default these are both faceted and available to be returned in search results)
		list.add(new IndexField().withIndexFieldName(FIELD_MODIFIED_ON).withIndexFieldType(IndexFieldType.Int));
		list.add(new IndexField().withIndexFieldName(FIELD_CREATED_ON).withIndexFieldType(IndexFieldType.Int));
		list.add(new IndexField().withIndexFieldName(FIELD_NUM_SAMPLES).withIndexFieldType(IndexFieldType.Int));
		// Literal text field facets with return disabled
		list.add(new IndexField().withIndexFieldName(FIELD_PARENT_ID).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(literalOptionsReturnDisabled));
		list.add(new IndexField().withIndexFieldName(FIELD_ACL).withIndexFieldType(IndexFieldType.LiteralArray).withLiteralArrayOptions(literalArrayOptionsReturnDisabled));
		list.add(new IndexField().withIndexFieldName(FIELD_UPDATE_ACL).withIndexFieldType(IndexFieldType.LiteralArray).withLiteralArrayOptions(literalArrayOptionsReturnDisabled));
		list.add(new IndexField().withIndexFieldName(FIELD_PLATFORM).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(literalOptionsReturnDisabled));
		list.add(new IndexField().withIndexFieldName(FIELD_REFERENCE).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(literalOptionsReturnDisabled));

		//Literal text field facets with return enabled
		list.add(new IndexField().withIndexFieldName(FIELD_TISSUE).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(literalOptionsReturnEnabled));
		list.add(new IndexField().withIndexFieldName(FIELD_CREATED_BY).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(literalOptionsReturnEnabled));
		list.add(new IndexField().withIndexFieldName(FIELD_DISEASE).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(literalOptionsReturnEnabled));
		list.add(new IndexField().withIndexFieldName(FIELD_MODIFIED_BY).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(literalOptionsReturnEnabled));
		list.add(new IndexField().withIndexFieldName(FIELD_NODE_TYPE).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(literalOptionsReturnEnabled));
		list.add(new IndexField().withIndexFieldName(FIELD_CONSORTIUM).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(literalOptionsReturnEnabled));

		SEARCH_SCHEMA_FIELDS = Collections.unmodifiableList(list);
	}


	public static List<IndexField> loadSearchDomainSchema() {
		return SEARCH_SCHEMA_FIELDS;
	}
}
