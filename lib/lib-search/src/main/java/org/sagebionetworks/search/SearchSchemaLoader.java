package org.sagebionetworks.search;

import static org.sagebionetworks.search.SearchConstants.*;
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

import com.amazonaws.services.cloudsearch.model.IndexField;
import com.amazonaws.services.cloudsearch.model.IndexFieldType;
import com.amazonaws.services.cloudsearch.model.LiteralOptions;
import com.amazonaws.services.cloudsearch.model.SourceAttribute;
import com.amazonaws.services.cloudsearch.model.SourceData;
import com.amazonaws.services.cloudsearch.model.SourceDataFunction;
import com.amazonaws.services.cloudsearch.model.TextOptions;
import com.amazonaws.services.cloudsearch.model.UIntOptions;

/**
 * Load the search schema.
 * 
 * 
 * @author jmhill
 *
 */
public class SearchSchemaLoader {
	
	/**
	 * For now we are defining the schema in code.
	 * We can serialize this to file in the future if we decide to do so.
	 * 
	 * @return
	 */
	public static List<IndexField> loadSearchDomainSchema() {
		List<IndexField> list = new LinkedList<IndexField>();
		// Literal fields to be returned in Search Results
		list.add(new IndexField().withIndexFieldName(FIELD_ID).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withResultEnabled(true).withSearchEnabled(true)));
		list.add(new IndexField().withIndexFieldName(FIELD_ETAG).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withResultEnabled(true).withSearchEnabled(true)));
		// Free text fields to be returned in Search Results
		list.add(new IndexField().withIndexFieldName(FIELD_NAME).withIndexFieldType(IndexFieldType.Text).withTextOptions(new TextOptions().withResultEnabled(true)));
		list.add(new IndexField().withIndexFieldName(FIELD_DESCRIPTION).withIndexFieldType(IndexFieldType.Text).withTextOptions(new TextOptions().withResultEnabled(true)));
		list.add(new IndexField().withIndexFieldName(FIELD_BOOST).withIndexFieldType(IndexFieldType.Text).withTextOptions(new TextOptions().withResultEnabled(false)));
		// Numeric fields (by default these are both faceted and available to be returned in search results)
		list.add(new IndexField().withIndexFieldName(FIELD_MODIFIED_ON).withIndexFieldType(IndexFieldType.Uint));
		list.add(new IndexField().withIndexFieldName(FIELD_CREATED_ON).withIndexFieldType(IndexFieldType.Uint));
		list.add(new IndexField().withIndexFieldName(FIELD_NUM_SAMPLES).withIndexFieldType(IndexFieldType.Uint));
		list.add(new IndexField().withIndexFieldName(FIELD_ANCESTORS).withIndexFieldType(IndexFieldType.Uint));
		// Literal text field facets
		list.add(new IndexField().withIndexFieldName(FIELD_PARENT_ID).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withSearchEnabled(true).withFacetEnabled(true)));
		list.add(new IndexField().withIndexFieldName(FIELD_ACL).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withSearchEnabled(true).withFacetEnabled(true)));
		list.add(new IndexField().withIndexFieldName(FIELD_UPDATE_ACL).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withSearchEnabled(true).withFacetEnabled(true)));
		list.add(new IndexField().withIndexFieldName(FIELD_CREATED_BY).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withSearchEnabled(true).withFacetEnabled(true)));
		list.add(new IndexField().withIndexFieldName(FIELD_MODIFIED_BY).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withSearchEnabled(true).withFacetEnabled(true)));
		list.add(new IndexField().withIndexFieldName(FIELD_DISEASE).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withSearchEnabled(true).withFacetEnabled(true)));
		list.add(new IndexField().withIndexFieldName(FIELD_NODE_TYPE).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withSearchEnabled(true).withFacetEnabled(true)));
		list.add(new IndexField().withIndexFieldName(FIELD_PLATFORM).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withSearchEnabled(true).withFacetEnabled(true)));
		list.add(new IndexField().withIndexFieldName(FIELD_REFERENCE).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withSearchEnabled(true).withFacetEnabled(true)));
		list.add(new IndexField().withIndexFieldName(FIELD_SPECIES).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withSearchEnabled(true).withFacetEnabled(true)));
		list.add(new IndexField().withIndexFieldName(FIELD_TISSUE).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withSearchEnabled(true).withFacetEnabled(true)));
		// All annotations are stored ad literals in this field.
//		list.add(new IndexField().withIndexFieldName(FIELD_ANNOTATIONS).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withSearchEnabled(true).withFacetEnabled(false).withResultEnabled(false)));
		//  Literal text fields to be returned in Search Results whose source is a facet
		list.add(new IndexField().withIndexFieldName(FIELD_CREATED_BY_R).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withResultEnabled(true)).withSourceAttributes(new SourceAttribute().withSourceDataFunction(SourceDataFunction.Copy).withSourceDataCopy(new SourceData().withSourceName(FIELD_CREATED_BY))));
		list.add(new IndexField().withIndexFieldName(FIELD_MODIFIED_BY_R).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withResultEnabled(true)).withSourceAttributes(new SourceAttribute().withSourceDataFunction(SourceDataFunction.Copy).withSourceDataCopy(new SourceData().withSourceName(FIELD_MODIFIED_BY))));
		list.add(new IndexField().withIndexFieldName(FIELD_NODE_TYPE_R).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withResultEnabled(true)).withSourceAttributes(new SourceAttribute().withSourceDataFunction(SourceDataFunction.Copy).withSourceDataCopy(new SourceData().withSourceName(FIELD_NODE_TYPE))));
		list.add(new IndexField().withIndexFieldName(FIELD_DISEASE_R).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withResultEnabled(true)).withSourceAttributes(new SourceAttribute().withSourceDataFunction(SourceDataFunction.Copy).withSourceDataCopy(new SourceData().withSourceName(FIELD_DISEASE))));
		list.add(new IndexField().withIndexFieldName(FIELD_TISSUE_R).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(new LiteralOptions().withResultEnabled(true)).withSourceAttributes(new SourceAttribute().withSourceDataFunction(SourceDataFunction.Copy).withSourceDataCopy(new SourceData().withSourceName(FIELD_TISSUE))));
		return list;
	}
}
