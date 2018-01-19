package org.sagebionetworks.search;

import com.amazonaws.services.cloudsearchdomain.model.Bucket;
import com.amazonaws.services.cloudsearchdomain.model.BucketInfo;
import com.amazonaws.services.cloudsearchdomain.model.Hits;
import com.amazonaws.services.cloudsearchdomain.model.QueryParser;
import com.amazonaws.services.cloudsearchdomain.model.SearchRequest;
import com.amazonaws.services.cloudsearchdomain.model.SearchResult;
import com.google.common.base.CharMatcher;
import org.apache.commons.lang.math.NumberUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.search.Document;
import org.sagebionetworks.repo.model.search.DocumentFields;
import org.sagebionetworks.repo.model.search.DocumentTypeNames;
import org.sagebionetworks.repo.model.search.Facet;
import org.sagebionetworks.repo.model.search.FacetConstraint;
import org.sagebionetworks.repo.model.search.FacetTypeNames;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.KeyValue;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.util.ValidateArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import static org.sagebionetworks.search.SearchConstants.FIELD_ACL;
import static org.sagebionetworks.search.SearchConstants.FIELD_CREATED_BY;
import static org.sagebionetworks.search.SearchConstants.FIELD_CREATED_BY_R;
import static org.sagebionetworks.search.SearchConstants.FIELD_CREATED_ON;
import static org.sagebionetworks.search.SearchConstants.FIELD_DESCRIPTION;
import static org.sagebionetworks.search.SearchConstants.FIELD_DISEASE;
import static org.sagebionetworks.search.SearchConstants.FIELD_DISEASE_R;
import static org.sagebionetworks.search.SearchConstants.FIELD_ETAG;
import static org.sagebionetworks.search.SearchConstants.FIELD_MODIFIED_BY;
import static org.sagebionetworks.search.SearchConstants.FIELD_MODIFIED_BY_R;
import static org.sagebionetworks.search.SearchConstants.FIELD_MODIFIED_ON;
import static org.sagebionetworks.search.SearchConstants.FIELD_NAME;
import static org.sagebionetworks.search.SearchConstants.FIELD_NODE_TYPE;
import static org.sagebionetworks.search.SearchConstants.FIELD_NODE_TYPE_R;
import static org.sagebionetworks.search.SearchConstants.FIELD_NUM_SAMPLES;
import static org.sagebionetworks.search.SearchConstants.FIELD_PLATFORM;
import static org.sagebionetworks.search.SearchConstants.FIELD_REFERENCE;
import static org.sagebionetworks.search.SearchConstants.FIELD_SPECIES;
import static org.sagebionetworks.search.SearchConstants.FIELD_TISSUE;
import static org.sagebionetworks.search.SearchConstants.FIELD_TISSUE_R;

public class SearchUtil{
	public static final Map<String, FacetTypeNames> FACET_TYPES;

	static {
		Map<String, FacetTypeNames> facetTypes = new HashMap<String, FacetTypeNames>();
		facetTypes.put(FIELD_NODE_TYPE, FacetTypeNames.LITERAL);
		facetTypes.put(FIELD_DISEASE, FacetTypeNames.LITERAL);
		facetTypes.put(FIELD_TISSUE, FacetTypeNames.LITERAL);
		facetTypes.put(FIELD_SPECIES, FacetTypeNames.LITERAL);
		facetTypes.put(FIELD_PLATFORM, FacetTypeNames.LITERAL);
		facetTypes.put(FIELD_CREATED_BY, FacetTypeNames.LITERAL);
		facetTypes.put(FIELD_MODIFIED_BY, FacetTypeNames.LITERAL);
		facetTypes.put(FIELD_REFERENCE, FacetTypeNames.LITERAL);
		facetTypes.put(FIELD_ACL, FacetTypeNames.LITERAL);
		facetTypes.put(FIELD_CREATED_ON, FacetTypeNames.DATE);
		facetTypes.put(FIELD_MODIFIED_ON, FacetTypeNames.DATE);
		facetTypes.put(FIELD_NUM_SAMPLES, FacetTypeNames.CONTINUOUS);
		FACET_TYPES = Collections.unmodifiableMap(facetTypes);
	}


	public static SearchRequest generateSearchRequest(SearchQuery searchQuery){
		ValidateArgument.required(searchQuery, "searchQuery");
		SearchRequest searchRequest = new SearchRequest();

		List<String> q = searchQuery.getQueryTerm();
		List<KeyValue> bq = searchQuery.getBooleanQuery();
		StringBuilder queryTermsStringBuilder = new StringBuilder();

		// clean up empty q
		if(q != null && q.size() == 1 && "".equals(q.get(0))) {
			q = null;
		}

		// test for minimum search requirements
		if (!(q != null && q.size() > 0) && !(bq != null && bq.size() > 0)) {
			throw new IllegalArgumentException(
					"Either one queryTerm or one booleanQuery must be defined");
		}

		// unstructured query terms into structured query terms
		if (q != null && q.size() > 0)
			queryTermsStringBuilder.append("(and " + joinQueries(q, " ") + ")");

		// boolean query into structured query terms
		if (bq != null && bq.size() > 0) {
			List<String> bqTerms = new ArrayList<String>();
			for (KeyValue pair : bq) {
				// this regex is pretty lame to have. need to work continuous into KeyValue model
				String value = pair.getValue();

				if(value.contains("*")){ //prefix queries are treated differently
					String prefixQuery = createPrefixQuery(value, pair.getKey());
					bqTerms.add(prefixQuery);
					continue;
				}

				//convert numeric ranges from 2011 cloudsearch syntax to 2013 syntax, for example: 200.. to [200,}
				if(value.contains("..")) {
					//TODO: remove this part once client stops using ".." notation for ranges
					String[] range = value.split("\\.\\.", -1);

					if(range.length != 2 ){
						throw new IllegalArgumentException("Numeric range is incorrectly formatted");
					}

					StringBuilder rangeStringBuilder = new StringBuilder();
					//left bound
					if(range[0].equals("")){
						rangeStringBuilder.append("{");
					}else{
						rangeStringBuilder.append("[" + range[0]);
					}

					//right bound
					rangeStringBuilder.append(",");
					if(range[1].equals("")){
						rangeStringBuilder.append("}");
					}else{
						rangeStringBuilder.append( range[1] + "]");
					}
					value = rangeStringBuilder.toString();
				}

				if((value.contains("{") || value.contains("["))
						&& (value.contains("}") || value.contains("]")) ){ //if is a continuous range such as [300,}
					bqTerms.add("(range field=" + pair.getKey()+ " " + value + ")");
					continue;
				}

				//add quotes around value. i.e. value -> 'value'
				value = "'" + escapeQuotedValue(pair.getValue()) + "'";
				String term = pair.getKey() + ":" + value;
				if(pair.getNot() != null && pair.getNot()) {
					term = "(not " + term + ")";
				}
				bqTerms.add(term);
			}

			//turns it from (and <q1> <q2> ... <qN>) into (and (and <q1> <q2> ... <qN>) <bqterm1> <bqterm2> ... <bqtermN>)
			queryTermsStringBuilder.append( (queryTermsStringBuilder.length() > 0 ? " ":"") + String.join(" ", bqTerms)+ ")");
			queryTermsStringBuilder.insert(0, "(and "); //add to the beginning of string
		}

		searchRequest.setQueryParser(QueryParser.Structured);
		searchRequest.setQuery(queryTermsStringBuilder.toString());

		//preprocess the FacetSortConstraints
		// facet field constraints
		if (searchQuery.getFacetFieldConstraints() != null
				&& searchQuery.getFacetFieldConstraints().size() > 0) {
			throw new IllegalArgumentException("Facet field constraints are no longer supported");
		}
		if (searchQuery.getFacetFieldSort() != null){
			throw new IllegalArgumentException("Sorting of facets is no longer supported");
		}

		// facets
		if (searchQuery.getFacet() != null && searchQuery.getFacet().size() > 0){ //iterate over all facets
			StringBuilder facetStringBuilder = new StringBuilder('{');
			int initialStrBuilderLen = facetStringBuilder.length();
			for(String facetFieldName : searchQuery.getFacet()){
				if (facetStringBuilder.length() > initialStrBuilderLen){
					facetStringBuilder.append(',');
				}
				//no options inside {} since none are used by the webclient
				facetStringBuilder.append("\""+ facetFieldName + "\":{}");
			}
			facetStringBuilder.append('}');
			searchRequest.setFacet(facetStringBuilder.toString());
		}

		//switch to size parameter in facet
		// facet top n
		if (searchQuery.getFacetFieldTopN() != null) {
			throw new IllegalArgumentException("facet-field-top-n is no longer supported");
		}

		// rank
		if (searchQuery.getRank() != null){
			throw new IllegalArgumentException("Rank is no longer supported");
		}


		// return-fields
		if (searchQuery.getReturnFields() != null
				&& searchQuery.getReturnFields().size() > 0)
			searchRequest.setReturn(String.join(",", searchQuery.getReturnFields()));

		// size
		if (searchQuery.getSize() != null)
			searchRequest.setSize(searchQuery.getSize());

		// start
		if (searchQuery.getStart() != null)
			searchRequest.setStart(searchQuery.getStart());

		return searchRequest;
	}

	public static SearchResults convertToSynapseSearchResult(SearchResult cloudSearchResult){
		SearchResults synapseSearchResults = new SearchResults();

		//Handle Translating of facets
		List<Facet> facetList = new ArrayList<>();
		Map<String, BucketInfo> facetMap = cloudSearchResult.getFacets();
		if(facetMap != null) {
			for (Map.Entry<String, BucketInfo> facetInfo : facetMap.entrySet()) {//iterate over each facet
				facetList.add(convertToSynapseSearchFacet(facetInfo.getKey(), facetInfo.getValue()));
			}
		}
		synapseSearchResults.setFacets(facetList);

		//Handle translation of Hits
		//class names are clashing feelsbadman
		List<org.sagebionetworks.repo.model.search.Hit> hitList = new ArrayList<>();
		Hits hits = cloudSearchResult.getHits();
		if (hits != null) {
			synapseSearchResults.setFound(hits.getFound());
			synapseSearchResults.setStart(hits.getStart());

			for (com.amazonaws.services.cloudsearchdomain.model.Hit cloudSearchHit : hits.getHit()) {
				hitList.add(convertToSynapseHit(cloudSearchHit));
			}
		}
		synapseSearchResults.setHits(hitList);

		return synapseSearchResults;
	}

	static Facet convertToSynapseSearchFacet(String facetName, BucketInfo bucketInfo) {
		FacetTypeNames facetType = FACET_TYPES.get(facetName);
		if (facetType == null) {
			throw new IllegalArgumentException(
					"facet "
							+ facetName
							+ " is not properly configured, add it to the facet type map");
		}

		Facet synapseFacet = new Facet();
		synapseFacet.setName(facetName);
		synapseFacet.setType(facetType);
		// Note: min and max are never set since the frontend never makes use of them and so the results won't ever have them.
		// A IllegalArgumentException would have been throw when converting from Synapse's SearchQuery to Amazon's SearchRequest

		List<FacetConstraint> facetConstraints = new ArrayList<>();
		for (Bucket bucket: bucketInfo.getBuckets()){
			FacetConstraint facetConstraint = new FacetConstraint();
			facetConstraint.setValue(bucket.getValue());
			facetConstraint.setCount(bucket.getCount());
			facetConstraints.add(facetConstraint);
		}
		synapseFacet.setConstraints(facetConstraints);
		return synapseFacet;
	}

	static org.sagebionetworks.repo.model.search.Hit convertToSynapseHit(com.amazonaws.services.cloudsearchdomain.model.Hit cloudSearchHit){
		Map<String, List<String>> fieldsMap = cloudSearchHit.getFields();

		org.sagebionetworks.repo.model.search.Hit synapseHit = new org.sagebionetworks.repo.model.search.Hit();
		synapseHit.setCreated_by(getFirstListValueFromMap(fieldsMap, FIELD_CREATED_BY_R));
		synapseHit.setCreated_on(NumberUtils.createLong(getFirstListValueFromMap(fieldsMap, FIELD_CREATED_ON)));
		synapseHit.setDescription(getFirstListValueFromMap(fieldsMap, FIELD_DESCRIPTION));
		synapseHit.setDisease(getFirstListValueFromMap(fieldsMap, FIELD_DISEASE_R));
		synapseHit.setEtag(getFirstListValueFromMap(fieldsMap, FIELD_ETAG));
		synapseHit.setModified_by(getFirstListValueFromMap(fieldsMap, FIELD_MODIFIED_BY_R));
		synapseHit.setModified_on(NumberUtils.createLong(getFirstListValueFromMap(fieldsMap, FIELD_MODIFIED_ON)));
		synapseHit.setName(getFirstListValueFromMap(fieldsMap, FIELD_NAME));
		synapseHit.setNode_type(getFirstListValueFromMap(fieldsMap, FIELD_NODE_TYPE_R));
		synapseHit.setNum_samples(NumberUtils.createLong(getFirstListValueFromMap(fieldsMap, FIELD_NUM_SAMPLES)));
		synapseHit.setTissue(getFirstListValueFromMap(fieldsMap, FIELD_TISSUE_R));
		//synapseHit.setPath() also exists but there does not appear to be a path field in the cloudsearch anymore.
		synapseHit.setId(cloudSearchHit.getId());
		return synapseHit;
	}


	static String getFirstListValueFromMap(Map<String, List<String>> map, String key){
		List<String> list = map.get(key);
		return (list == null || list.isEmpty()) ? null : list.get(0);
	}

	/*
	 * Private Methods
	 */
	/**
	 * Creates a prefix query if there is an asterisk
	 * @param prefixStringWithAsterisk prefix string containing the * symbol
	 * @param fieldName optional. used in boolean queries but not in regular queries. 
	 * @return
	 */
	private static String createPrefixQuery(String prefixStringWithAsterisk, String fieldName){
		int asteriskIndex = prefixStringWithAsterisk.indexOf('*');
		if(asteriskIndex == -1){
			throw new IllegalArgumentException("the prefixString does not contain an * (asterisk) symbol");
		}
		return "(prefix" + (fieldName==null ? "" : " field=" + fieldName) + " '" + prefixStringWithAsterisk.substring(0, asteriskIndex) + "')";
	}
	
	private static String joinQueries(List<String> list, String delimiter){
		StringBuilder sb = new StringBuilder();
		for (String item : list) {
			if(item.contains("*")){
				sb.append(createPrefixQuery(item, null));
			}else{
				sb.append('\''); //appends ' character
				sb.append(item);
				sb.append('\'');
			}
			sb.append(delimiter);
		}
		String str = sb.toString();
		if (str.length() > 0) {
			str = str.substring(0, str.length()-1);
		}
		return str;
	}

	private static String escapeQuotedValue(String value) {
		value = value.replaceAll("\\\\", "\\\\\\\\"); // replace \ -> \\
		value = value.replaceAll("'", "\\\\'"); // replace ' -> \'
		return value;
	}


	/**
	 * Formulate the boolean query to enforce an access controll list for a
	 * particular user
	 *
	 * @param userInfo
	 * @return the acl boolean query
	 * @throws DatastoreException
	 */
	public static String formulateAuthorizationFilter(UserInfo userInfo)
			throws DatastoreException {
		ValidateArgument.required(userInfo, "userInfo");
		Set<Long> userGroups = userInfo.getGroups();
		ValidateArgument.required(userGroups, "userInfo.getGroups()");
		ValidateArgument.requirement(!userGroups.isEmpty(), "no groups for user " + userInfo);

		// Make our boolean query
		StringBuilder authorizationFilterBuilder = new StringBuilder("(or ");
		int initialLen = authorizationFilterBuilder.length();
		for (Long group : userGroups) {
			if (authorizationFilterBuilder.length() > initialLen) {
				authorizationFilterBuilder.append(" ");
			}
			authorizationFilterBuilder.append(FIELD_ACL).append(":'").append(group).append("'");
		}
		authorizationFilterBuilder.append(")");

		return authorizationFilterBuilder.toString();
	}

	/**
	 * Remove any character that is not compatible with cloud search.
	 * @param documents
	 * @return JSON String of cleaned document
	 */
	static String convertSearchDocumentsToJSONString(List<Document> documents) {
		ValidateArgument.required(documents, "documents");
		// CloudSearch will not accept an empty document batch
		ValidateArgument.requirement(!documents.isEmpty(), "documents can not be empty");

		StringJoiner stringJoiner = new StringJoiner(", ", "[", "]");

		for (Document document : documents) {
			if (DocumentTypeNames.add == document.getType()) {
				prepareDocument(document);
			}
			try {
				stringJoiner.add(EntityFactory.createJSONStringForEntity(document));
			} catch (JSONObjectAdapterException e) {
				throw new RuntimeException(e);
			}
		}
		// Some descriptions have control characters in them for some reason, in any case, just get rid
		// of all control characters in the search document
		return CharMatcher.JAVA_ISO_CONTROL.removeFrom(stringJoiner.toString());
	}

	/**
	 * Prepare the document to be sent.
	 * @param document
	 */
	static void prepareDocument(Document document) {
		if(document.getFields() == null){
			document.setFields(new DocumentFields());
		}
		// The id field must match the document's id.
		document.getFields().setId(document.getId());
	}
}
