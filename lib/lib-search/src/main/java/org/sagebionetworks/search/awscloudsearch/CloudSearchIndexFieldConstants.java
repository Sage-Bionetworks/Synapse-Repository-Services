package org.sagebionetworks.search.awscloudsearch;

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

import com.amazonaws.services.cloudsearchv2.model.IndexField;
import com.amazonaws.services.cloudsearchv2.model.IndexFieldType;
import com.amazonaws.services.cloudsearchv2.model.IntOptions;
import com.amazonaws.services.cloudsearchv2.model.LiteralArrayOptions;
import com.amazonaws.services.cloudsearchv2.model.LiteralOptions;
import com.amazonaws.services.cloudsearchv2.model.TextArrayOptions;
import com.amazonaws.services.cloudsearchv2.model.TextOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class CloudSearchIndexFieldConstants {

		/*
		 * CloudSearchV2 defaults to this analysis scheme if not specified,
		 * but specifying it will make equals() function for comparing
		 * the created IndexFields to ones retrieved from AWS work.
		 */
		private static String DEFAULT_TEXT_ANALYSIS_SCHEME = "_en_default_";

		//can search values, faceting enabled for this index. index values not be returned in search results.
		private static LiteralOptions literalOptionsReturnDisabled = new LiteralOptions().withSearchEnabled(true).withFacetEnabled(true).withReturnEnabled(false);

		//can search values, faceting disabled, return disabled
		private static LiteralArrayOptions literalArrayOptionsFacetDisabledReturnDisabled = new LiteralArrayOptions().withSearchEnabled(true).withFacetEnabled(false).withReturnEnabled(false);

		//search, facet, and return all enabled
		private static LiteralOptions literalOptionsReturnEnabled = new LiteralOptions().withReturnEnabled(true).withSearchEnabled(true).withFacetEnabled(true);
		private static LiteralArrayOptions literalArrayOptionsReturnEnabled = new LiteralArrayOptions().withReturnEnabled(true).withSearchEnabled(true).withFacetEnabled(true);
		private static IntOptions intOptionsReturnEnabled = new IntOptions().withReturnEnabled(true).withSearchEnabled(true).withFacetEnabled(true);

		// faceting is disabled because all values are unique
		private static LiteralOptions literalOptionsFacetDisabled = new LiteralOptions().withReturnEnabled(true).withSearchEnabled(true).withFacetEnabled(false);


		// Literal fields to be returned in Search Results
		static IndexField INDEX_FIELD_ETAG = new IndexField().withIndexFieldName(FIELD_ETAG).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(literalOptionsFacetDisabled);
		// Free text fields to be returned in Search Results
		static IndexField INDEX_FIELD_NAME = new IndexField().withIndexFieldName(FIELD_NAME).withIndexFieldType(IndexFieldType.Text).withTextOptions(new TextOptions().withReturnEnabled(true).withAnalysisScheme(DEFAULT_TEXT_ANALYSIS_SCHEME));
		static IndexField INDEX_FIELD_DESCRIPTION = new IndexField().withIndexFieldName(FIELD_DESCRIPTION).withIndexFieldType(IndexFieldType.Text).withTextOptions(new TextOptions().withReturnEnabled(true).withAnalysisScheme(DEFAULT_TEXT_ANALYSIS_SCHEME));
		static IndexField INDEX_FIELD_BOOST = new IndexField().withIndexFieldName(FIELD_BOOST).withIndexFieldType(IndexFieldType.TextArray).withTextArrayOptions(new TextArrayOptions().withReturnEnabled(false).withAnalysisScheme(DEFAULT_TEXT_ANALYSIS_SCHEME));
		// Numeric fields (by default these are both faceted and available to be returned in search results)
		static IndexField INDEX_FIELD_MODIFIED_ON = new IndexField().withIndexFieldName(FIELD_MODIFIED_ON).withIndexFieldType(IndexFieldType.Int).withIntOptions(intOptionsReturnEnabled);
		static IndexField INDEX_FIELD_CREATED_ON = new IndexField().withIndexFieldName(FIELD_CREATED_ON).withIndexFieldType(IndexFieldType.Int).withIntOptions(intOptionsReturnEnabled);
		static IndexField INDEX_FIELD_NUM_SAMPLES = new IndexField().withIndexFieldName(FIELD_NUM_SAMPLES).withIndexFieldType(IndexFieldType.Int).withIntOptions(intOptionsReturnEnabled);
		// Literal text field facets with return disabled
		static IndexField INDEX_FIELD_PARENT_ID = new IndexField().withIndexFieldName(FIELD_PARENT_ID).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(literalOptionsReturnDisabled);
		static IndexField INDEX_FIELD_PLATFORM = new IndexField().withIndexFieldName(FIELD_PLATFORM).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(literalOptionsReturnDisabled);
		static IndexField INDEX_FIELD_REFERENCE = new IndexField().withIndexFieldName(FIELD_REFERENCE).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(literalOptionsReturnDisabled);

		//Literal text field facets with return enabled
		static IndexField INDEX_FIELD_TISSUE = new IndexField().withIndexFieldName(FIELD_TISSUE).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(literalOptionsReturnEnabled);
		static IndexField INDEX_FIELD_CREATED_BY = new IndexField().withIndexFieldName(FIELD_CREATED_BY).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(literalOptionsReturnEnabled);
		static IndexField INDEX_FIELD_DISEASE = new IndexField().withIndexFieldName(FIELD_DISEASE).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(literalOptionsReturnEnabled);
		static IndexField INDEX_FIELD_MODIFIED_BY = new IndexField().withIndexFieldName(FIELD_MODIFIED_BY).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(literalOptionsReturnEnabled);
		static IndexField INDEX_FIELD_NODE_TYPE = new IndexField().withIndexFieldName(FIELD_NODE_TYPE).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(literalOptionsReturnEnabled);
		static IndexField INDEX_FIELD_CONSORTIUM = new IndexField().withIndexFieldName(FIELD_CONSORTIUM).withIndexFieldType(IndexFieldType.Literal).withLiteralOptions(literalOptionsReturnEnabled);

		//faceting and return disabled
		static IndexField INDEX_FIELD_ACL = new IndexField().withIndexFieldName(FIELD_ACL).withIndexFieldType(IndexFieldType.LiteralArray).withLiteralArrayOptions(literalArrayOptionsFacetDisabledReturnDisabled);
		static IndexField INDEX_FIELD_UPDATE_ACL = new IndexField().withIndexFieldName(FIELD_UPDATE_ACL).withIndexFieldType(IndexFieldType.LiteralArray).withLiteralArrayOptions(literalArrayOptionsFacetDisabledReturnDisabled);



}
