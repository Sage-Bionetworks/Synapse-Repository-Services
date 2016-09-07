package org.sagebionetworks.search;

import static org.sagebionetworks.search.SearchConstants.FIELD_ACL;
import static org.sagebionetworks.search.SearchConstants.FIELD_ANCESTORS;
import static org.sagebionetworks.search.SearchConstants.FIELD_BOOST;
import static org.sagebionetworks.search.SearchConstants.FIELD_CREATED_BY;
import static org.sagebionetworks.search.SearchConstants.FIELD_CREATED_BY_R;
import static org.sagebionetworks.search.SearchConstants.FIELD_CREATED_ON;
import static org.sagebionetworks.search.SearchConstants.FIELD_DESCRIPTION;
import static org.sagebionetworks.search.SearchConstants.FIELD_DISEASE;
import static org.sagebionetworks.search.SearchConstants.FIELD_DISEASE_R;
import static org.sagebionetworks.search.SearchConstants.FIELD_ETAG;
import static org.sagebionetworks.search.SearchConstants.FIELD_ID;
import static org.sagebionetworks.search.SearchConstants.FIELD_MODIFIED_BY;
import static org.sagebionetworks.search.SearchConstants.FIELD_MODIFIED_BY_R;
import static org.sagebionetworks.search.SearchConstants.FIELD_MODIFIED_ON;
import static org.sagebionetworks.search.SearchConstants.FIELD_NAME;
import static org.sagebionetworks.search.SearchConstants.FIELD_NODE_TYPE;
import static org.sagebionetworks.search.SearchConstants.FIELD_NODE_TYPE_R;
import static org.sagebionetworks.search.SearchConstants.FIELD_NUM_SAMPLES;
import static org.sagebionetworks.search.SearchConstants.FIELD_PARENT_ID;
import static org.sagebionetworks.search.SearchConstants.FIELD_PLATFORM;
import static org.sagebionetworks.search.SearchConstants.FIELD_REFERENCE;
import static org.sagebionetworks.search.SearchConstants.FIELD_SPECIES;
import static org.sagebionetworks.search.SearchConstants.FIELD_TISSUE;
import static org.sagebionetworks.search.SearchConstants.FIELD_TISSUE_R;
import static org.sagebionetworks.search.SearchConstants.FIELD_UPDATE_ACL;

import java.util.LinkedList;
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
		List<IndexField> list = new LinkedList<IndexField>();
		// Literal fields to be returned in Search Results
		list.add(new IndexField().withIndexFieldName(FIELD_ID).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withReturnEnabled(true).withSearchEnabled(true).withFacetEnabled(false)));
		list.add(new IndexField().withIndexFieldName(FIELD_ETAG).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withReturnEnabled(true).withSearchEnabled(true).withFacetEnabled(false)));
		// Free text fields to be returned in Search Results
		list.add(new IndexField().withIndexFieldName(FIELD_NAME).withIndexFieldType(IndexFieldType.Text).withTextOptions(new TextOptions().withReturnEnabled(true).withAnalysisScheme(DEFAULT_TEXT_ANALYSIS_SCHEME)));
		list.add(new IndexField().withIndexFieldName(FIELD_DESCRIPTION).withIndexFieldType(IndexFieldType.Text).withTextOptions(new TextOptions().withReturnEnabled(true).withAnalysisScheme(DEFAULT_TEXT_ANALYSIS_SCHEME)));
		list.add(new IndexField().withIndexFieldName(FIELD_BOOST).withIndexFieldType(IndexFieldType.TextArray).withTextArrayOptions(new TextArrayOptions().withReturnEnabled(false).withAnalysisScheme(DEFAULT_TEXT_ANALYSIS_SCHEME)));
		// Numeric fields (by default these are both faceted and available to be returned in search results)
		list.add(new IndexField().withIndexFieldName(FIELD_MODIFIED_ON).withIndexFieldType(IndexFieldType.Int));
		list.add(new IndexField().withIndexFieldName(FIELD_CREATED_ON).withIndexFieldType(IndexFieldType.Int));
		list.add(new IndexField().withIndexFieldName(FIELD_NUM_SAMPLES).withIndexFieldType(IndexFieldType.IntArray));
		list.add(new IndexField().withIndexFieldName(FIELD_ANCESTORS).withIndexFieldType(IndexFieldType.IntArray));
		// Literal text field facets
		list.add(new IndexField().withIndexFieldName(FIELD_PARENT_ID).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withSearchEnabled(true).withFacetEnabled(true).withReturnEnabled(false)));
		list.add(new IndexField().withIndexFieldName(FIELD_ACL).withIndexFieldType(IndexFieldType.LiteralArray).withLiteralArrayOptions(new LiteralArrayOptions().withSearchEnabled(true).withFacetEnabled(true).withReturnEnabled(false)));
		list.add(new IndexField().withIndexFieldName(FIELD_UPDATE_ACL).withIndexFieldType(IndexFieldType.LiteralArray).withLiteralArrayOptions(new LiteralArrayOptions().withSearchEnabled(true).withFacetEnabled(true).withReturnEnabled(false)));
		list.add(new IndexField().withIndexFieldName(FIELD_CREATED_BY).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withSearchEnabled(true).withFacetEnabled(true).withReturnEnabled(false)));
		list.add(new IndexField().withIndexFieldName(FIELD_MODIFIED_BY).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withSearchEnabled(true).withFacetEnabled(true).withReturnEnabled(false)));
		list.add(new IndexField().withIndexFieldName(FIELD_DISEASE).withIndexFieldType(IndexFieldType.LiteralArray).withLiteralArrayOptions(new LiteralArrayOptions().withSearchEnabled(true).withFacetEnabled(true).withReturnEnabled(false)));
		list.add(new IndexField().withIndexFieldName(FIELD_NODE_TYPE).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withSearchEnabled(true).withFacetEnabled(true).withReturnEnabled(false)));
		list.add(new IndexField().withIndexFieldName(FIELD_PLATFORM).withIndexFieldType(IndexFieldType.LiteralArray).withLiteralArrayOptions(new LiteralArrayOptions().withSearchEnabled(true).withFacetEnabled(true).withReturnEnabled(false)));
		list.add(new IndexField().withIndexFieldName(FIELD_REFERENCE).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withSearchEnabled(true).withFacetEnabled(true).withReturnEnabled(false)));
		list.add(new IndexField().withIndexFieldName(FIELD_SPECIES).withIndexFieldType(IndexFieldType.LiteralArray).withLiteralArrayOptions(new LiteralArrayOptions().withSearchEnabled(true).withFacetEnabled(true).withReturnEnabled(false)));
		list.add(new IndexField().withIndexFieldName(FIELD_TISSUE).withIndexFieldType(IndexFieldType.LiteralArray).withLiteralArrayOptions(new LiteralArrayOptions().withSearchEnabled(true).withFacetEnabled(true).withReturnEnabled(false)));
		// All annotations are stored ad literals in this field.
//		list.add(new IndexField().withIndexFieldName(FIELD_ANNOTATIONS).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withSearchEnabled(true).withFacetEnabled(false).withResultEnabled(false)));
		//  Literal text fields to be returned in Search Results whose source is a facet
		list.add(new IndexField().withIndexFieldName(FIELD_CREATED_BY_R).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withReturnEnabled(true).withSourceField(FIELD_CREATED_BY).withSearchEnabled(false).withFacetEnabled(false)));
		list.add(new IndexField().withIndexFieldName(FIELD_MODIFIED_BY_R).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withReturnEnabled(true).withSourceField(FIELD_MODIFIED_BY).withSearchEnabled(false).withFacetEnabled(false)));
		list.add(new IndexField().withIndexFieldName(FIELD_NODE_TYPE_R).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withReturnEnabled(true).withSourceField(FIELD_NODE_TYPE).withSearchEnabled(false).withFacetEnabled(false)));
		list.add(new IndexField().withIndexFieldName(FIELD_DISEASE_R).withIndexFieldType(IndexFieldType.LiteralArray).withLiteralArrayOptions(new LiteralArrayOptions().withReturnEnabled(true).withSourceFields(FIELD_DISEASE).withSearchEnabled(false).withFacetEnabled(false)));
		list.add(new IndexField().withIndexFieldName(FIELD_TISSUE_R).withIndexFieldType(IndexFieldType.LiteralArray).withLiteralArrayOptions(new LiteralArrayOptions().withReturnEnabled(true).withSourceFields(FIELD_TISSUE).withSearchEnabled(false).withFacetEnabled(false)));
		return list;
	}
}
